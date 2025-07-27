package com.sastudios.gatekeeper.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.sastudios.gatekeeper.model.Operation
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class KafkaBroadcastQueue(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val jackson: ObjectMapper
) : OperationBroadcastQueue {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun publish(op: Operation) {
        try {
            val wrapper = mapOf(
                "type" to "operation",
                "payload" to op
            )

            val json = jackson.writeValueAsString(wrapper)
            kafkaTemplate.send("doc-changes", op.docId, json)

//            val json = jackson.writeValueAsString(op)
//            kafkaTemplate.send("doc-changes", op.docId, json)
                .whenComplete { result, ex ->
                    if (ex != null) {
                        logger.error(" Failed to publish operation to Kafka for docId=${op.docId}", ex)
                        // Consider adding retry logic or dead-letter queue here
                    } else {
                        logger.info("Successfully published operation to Kafka for docId=${op.docId}")
                    }
                }

        } catch (ex: Exception) {
            logger.error("Failed to serialize or send operation: ${ex.message}")
            throw ex
            // Optional: fallback to local in-memory queue or retry queue
        }
    }
}