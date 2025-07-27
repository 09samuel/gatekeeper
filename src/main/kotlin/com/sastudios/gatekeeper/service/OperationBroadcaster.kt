package com.sastudios.gatekeeper.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.sastudios.gatekeeper.model.Operation
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import java.util.concurrent.ConcurrentHashMap

@Component
class OperationBroadcaster(
    private val localSessions: ConcurrentHashMap<String, MutableSet<WebSocketSession>>,
    private val jackson: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @KafkaListener(topics = ["doc-changes"], groupId = "editor-group", concurrency = "5")
    fun onMessage(message: String) {
        try {
//            val op = jackson.readValue(message, Operation::class.java)
            val node = jackson.readTree(message)
            if (node["type"].asText() == "operation") {
                val op = jackson.treeToValue(node["payload"], Operation::class.java)
                // broadcast it
                localSessions[op.docId]?.forEach { session ->

                    // Skip sender
//                val sessionUserId = session.attributes["userId"] as? String
//                if (sessionUserId == op.userId) return@forEach

                    //isolate tabs on browser
                    val connId = session.attributes["connId"] as? String
                    if (connId == op.senderConnId) return@forEach

                    try {
                        if (session.isOpen) {
                            session.sendMessage(TextMessage(message))
                        } else {
                            logger.warn("WebSocket session for docId=${op.docId} is closed")
                        }
                    } catch (wsEx: Exception) {
                        logger.error("Failed to send message over WebSocket: ${wsEx.message}")
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to process Kafka message: ${e.message}\nPayload: $message")
        }
    }
}
