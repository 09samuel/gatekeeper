package com.sastudios.gatekeeper.websocket

import com.fasterxml.jackson.databind.ObjectMapper
import com.sastudios.gatekeeper.entity.CassandraOperation
import com.sastudios.gatekeeper.model.Operation
import com.sastudios.gatekeeper.repository.CassandraOperationRepository
import com.sastudios.gatekeeper.repository.DocumentRepository
import com.sastudios.gatekeeper.security.JwtService
import com.sastudios.gatekeeper.service.DocumentSyncService
import com.sastudios.gatekeeper.service.OperationBroadcastQueue
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import com.sastudios.gatekeeper.service.OperationalTransformer
import com.sastudios.gatekeeper.service.RedissonLockService
import com.sastudios.gatekeeper.service.S3Service
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlinx.coroutines.withTimeoutOrNull
import org.springframework.kafka.core.KafkaTemplate
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Component
class DocumentWebSocketHandler(
    private val transformer: OperationalTransformer,
    private val cassRepo: CassandraOperationRepository,
    private val kafka: KafkaTemplate<String, CompactionTrigger>,
    private val jackson: ObjectMapper,
    private val sessionsMap: ConcurrentHashMap<String, MutableSet<WebSocketSession>>,
    private val redissonLockService: RedissonLockService,
//    private val docOwnershipManager: DocOwnershipManager,
    private val broadcastQueue: OperationBroadcastQueue,
    private val documentRepository: DocumentRepository,
    private val jwtService: JwtService,
    private val documentSyncService: DocumentSyncService,
    private val objectMapper: ObjectMapper
) : TextWebSocketHandler() {

    private val logger = LoggerFactory.getLogger(this::class.java)
    private val wsScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun afterConnectionEstablished(session: WebSocketSession) {
        val uri = session.uri ?: run {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Missing URI"))
            return
        }

        val docId = uri.path?.substringAfterLast("/") ?: run {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Missing docId"))
            return
        }

        if (!documentRepository.existsById(docId.toLong())) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Invalid docId"))
            return
        }

        val token = uri.query
            ?.split("&")
            ?.firstOrNull { it.startsWith("token=") }
            ?.substringAfter("=")

        if (token.isNullOrBlank()) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Missing access token"))
            return
        }

        try {
            val userId = jwtService.getUserIdFromToken(token)

            session.attributes["userId"] = userId
            session.attributes["connId"] = UUID.randomUUID().toString()

            sessionsMap.computeIfAbsent(docId) { ConcurrentHashMap.newKeySet() }.add(session)
            logger.info("WebSocket connection established for docId=$docId, userId=$userId")

            runBlocking(Dispatchers.IO) {
                try {
                    documentSyncService.handleJoin(session, docId)
                } catch (ex: Exception) {
                    logger.error("Failed to send snapshot for docId=$docId: ${ex.message}")
                    val errMsg = mapOf("type" to "error", "message" to "Snapshot error: ${ex.message}")
                    session.sendMessage(TextMessage(objectMapper.writeValueAsString(errMsg)))
                }
            }


        } catch (e: Exception) {
            logger.error("Token verification failed: ${e.message}")
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Invalid access token"))
        }
    }


    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        val rawPath = session.uri?.path
        if (rawPath.isNullOrBlank()) {
            logger.warn("WebSocket closed but URI path was null or blank.")
            return
        }

        val docId = rawPath.substringAfterLast("/", missingDelimiterValue = "").trim()

        val sessions = sessionsMap[docId]

        if (sessions == null) {
            logger.warn("No session set found for docId=$docId")
            return
        }

        try {
            sessions.remove(session)
            logger.info("Removed WebSocket session from docId=$docId, remaining=${sessions.size}")

            // If no sessions remain for this doc, clean up
            if (sessions.isEmpty()) {
                val latestRevision = documentRepository.findLatestRevisionById(docId.toLong()) ?: 0
                kafka.send("doc-close", CompactionTrigger(docId, latestRevision)).whenComplete { result, ex ->
                    if (ex != null) {
                        logger.error(" Sent compaction trigger for docId=$docId, rev=$latestRevision")
                        // Consider adding retry logic or dead-letter queue here
                    } else {
                        logger.info("Failed to send compaction trigger for docId=$docId:")
                    }
                }
//            docOwnershipManager.release(docId)  // delete Redis key: doc:<docId>
            }
        } catch (e: Exception) {
            logger.error("Error while finalizing docId=$docId on last disconnect: ${e.message}")
        }
    }

    override fun handleTextMessage(ws: WebSocketSession, msg: TextMessage) {
        wsScope.launch {
            try {
                val op = jackson.readValue(msg.payload, Operation::class.java)
                validateOperation(op)

                val actualUserId = ws.attributes["userId"] as? String
                    ?: throw IllegalStateException("Missing userId in WebSocket session")

                val actualConnId  = ws.attributes["connId"] as? String
                    ?: throw IllegalStateException("Missing connId")

                val opWithUserId = op.copy(
                    userId = actualUserId,
                    senderConnId = actualConnId
                )

                val lockKey = "lock:doc:${op.docId}"
                redissonLockService.withLock(lockKey) {
                    val history = cassRepo.findByDocIdAndRevisionGreaterThan(opWithUserId.docId, opWithUserId.baseRevision)
                        .map {
                            Operation(
                                it.docId,
                                it.userId,
                                it.baseRevision,
                                it.revision,
                                it.delta
                            )
                        }

                    val transformed = retryTransform(opWithUserId, history, maxRetries = 3)

                    cassRepo.save(
                        CassandraOperation(
                            docId = transformed.docId,
                            userId = transformed.userId,
                            baseRevision = transformed.baseRevision,
                            revision = transformed.revision,
                            delta = transformed.delta
                        )
                    )


                    broadcastQueue.publish(transformed)
                }
            } catch (ex: Exception) {
                val errorMessage = mapOf(
                    "type" to "error",
                    "message" to ex.message
                )
                ws.sendMessage(TextMessage(jackson.writeValueAsString(errorMessage)))
            }
        }
    }

    private fun validateOperation(op: Operation) {
        val delta = op.delta

        when {
            // Insert: "5:Hello"
            Regex("""^\d+:.+""").matches(delta) -> return

            // Delete: "5-del:3"
            Regex("""^\d+-del:\d+$""").matches(delta) -> return

            // Replace: "5-rep:3:Text"
            Regex("""^\d+-rep:\d+:.+""").matches(delta) -> return

            else -> throw IllegalArgumentException("Invalid delta format: $delta")
        }
    }


    private fun retryTransform(
        op: Operation,
        history: List<Operation>,
        maxRetries: Int
    ): Operation {
        var attempt = 0
        var lastError: Exception? = null
        while (attempt < maxRetries) {
            try {
                return transformer.transform(op, history)
            } catch (e: Exception) {
                lastError = e
                attempt++
            }
        }
        throw IllegalStateException("Transformation failed after $maxRetries retries", lastError)
    }

    data class CompactionTrigger(
        val docId: String,
        val latestRevision: Int
    )


//        localSessions[op.docId]?.forEach { session ->
//            if (session.isOpen) {
//                session.sendMessage(TextMessage(toJson(transformedOp)))
//            }
//        }

//        kafka.send("doc-changes", transformed)
}
