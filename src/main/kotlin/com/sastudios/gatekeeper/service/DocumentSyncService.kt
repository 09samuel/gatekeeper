package com.sastudios.gatekeeper.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.sastudios.gatekeeper.entity.CassandraOperation
import com.sastudios.gatekeeper.model.Operation
import com.sastudios.gatekeeper.repository.CassandraOperationRepository
import com.sastudios.gatekeeper.repository.DocumentRepository
import com.sastudios.gatekeeper.websocket.applyDelta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.cassandra.core.CassandraTemplate
import org.springframework.stereotype.Service
import org.springframework.web.socket.CloseStatus
import org.springframework.data.cassandra.core.query.Criteria
import org.springframework.data.cassandra.core.query.CriteriaDefinitions
import org.springframework.data.cassandra.core.query.Query
import org.springframework.data.cassandra.core.query.and
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession

@Service
class DocumentSyncService(
    private val documentRepository: DocumentRepository,
    private val cassRepo: CassandraOperationRepository,
    private val s3Service: S3Service,
    private val redissonLockService: RedissonLockService,
    private val cassandraTemplate: CassandraTemplate
) {

    suspend fun handleJoin(ws: WebSocketSession, docId: String) {
        val logger = LoggerFactory.getLogger("DocumentJoinHandler")

        val userId = ws.attributes["userId"] as? String
        if (userId.isNullOrBlank()) {
            logger.warn("WebSocket session has no userId for docId=$docId")
            ws.close(CloseStatus.NOT_ACCEPTABLE.withReason("Unauthenticated WebSocket"))
            return
        }

        val lockKey = "lock:compact:$docId"

        redissonLockService.withLock(lockKey) {
            try {

                val meta = documentRepository.findById(docId.toLong())
                    .orElseThrow { IllegalArgumentException("Document not found: $docId") }

                // Load base content from S3 (with timeout safety)
                val baseContent = withTimeoutOrNull(3_000) {
                    s3Service.getPlainText("https://collab-docs-bucket.s3.ap-south-1.amazonaws.com/documents/${docId}.txt")
                } ?: throw IllegalStateException("Failed to load base document from S3 for $docId")

                // Fetch un-compacted ops from Cassandra (if any)
                val query = Query.query(
                    Criteria.where("docId").`is`(docId)
                ).and(
                    Criteria.where("revision").gt(meta.compactedRevision)
                )

                val cassandraOps = cassandraTemplate.select(query, CassandraOperation::class.java)

                val operations = cassandraOps.map {
                    Operation(
                        docId = it.docId,
                        userId = it.userId,
                        baseRevision = it.baseRevision,
                        revision = it.revision,
                        delta = it.delta
                    )
                }.sortedBy { it.revision }

//                val operations = withTimeoutOrNull(3_000) {
//                    cassRepo.findByDocIdAndRevisionGreaterThan(docId, meta.compactedRevision)
//                        .map {
//                            Operation(
//                                docId = it.docId,
//                                userId = it.userId,
//                                baseRevision = it.baseRevision,
//                                revision = it.revision,
//                                delta = it.delta
//                            )
//                        }.sortedBy { it.revision }
//                } ?: throw IllegalStateException("Failed to fetch ops from Cassandra for $docId")



                val finalContent = try {
                    operations.fold(baseContent) { acc, op ->
                        applyDelta(acc, op.delta)
                    }
                } catch (e: Exception) {
                    logger.error("Delta replay failed for docId=$docId: ${e.message}")
                    throw IllegalStateException("Delta replay failed. Please refresh.")
                }

//                val revision = meta.compactedRevision + operations.size

//                val snapshotMessage = mapOf(
//                    "type" to "snapshot",
//                    "payload" to mapOf(
//                        "docId" to docId,
//                        "content" to finalContent,
//                        "revision" to revision
//                    )
//                )

                sendSafeJson(ws, finalContent, logger)


                logger.info("Sent snapshot for docId=$docId to userId=$userId (ops=${operations.size})")

            } catch (e: Exception) {
                logger.error("Failed to handle join for docId=$docId: ${e.message}")
                sendSafeJson(ws, mapOf("type" to "error", "message" to e.message), logger)
            }
        }
    }


    fun sendSafeJson(ws: WebSocketSession, payload: Any, logger: Logger) {
        try {
            if (ws.isOpen) {

                val json = ObjectMapper().writeValueAsString(payload)
                ws.sendMessage(TextMessage(json))
            }
        } catch (ex: Exception) {
            logger.warn("Failed to send message to WebSocket: ${ex.message}")
        }
    }

    suspend fun <T> runCatchingWithTimeout(timeoutMillis: Long, block: suspend () -> T): T? {
        return try {
            withTimeoutOrNull(timeoutMillis) {
                block()
            }
        } catch (ex: Exception) {
            println("Exception during runCatchingWithTimeout: ${ex.message}")
            ex.printStackTrace()
            null
        }
    }
}
