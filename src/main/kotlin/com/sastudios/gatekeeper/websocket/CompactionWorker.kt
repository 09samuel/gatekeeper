package com.sastudios.gatekeeper.websocket

import com.sastudios.gatekeeper.model.Operation
import com.sastudios.gatekeeper.repository.CassandraOperationRepository
import com.sastudios.gatekeeper.repository.DocumentRepository
import com.sastudios.gatekeeper.service.RedissonLockService
import com.sastudios.gatekeeper.service.S3Service
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CompactionWorker(
    private val kafkaConsumerFactory: ConsumerFactory<String, Operation>,
    private val metaRepo: DocumentRepository,
    private val cassRepo: CassandraOperationRepository,
    private val s3: S3Service,
    private val redissonLockService: RedissonLockService,
) {

    companion object {
        const val NUM_PARTITIONS = 16
    }

    private val logger = LoggerFactory.getLogger(this::class.java)

    @KafkaListener(topics = ["doc-close"], groupId = "compaction-workers", concurrency = "5")
    suspend fun onDocClose(trigger: DocumentWebSocketHandler.CompactionTrigger) {
        val docId = trigger.docId
        val lockKey = "lock:compact:$docId"

        try {
            redissonLockService.withLock(lockKey) {
                compactDocument(docId, trigger.latestRevision)
            }
        } catch (e: Exception) {
            //logger.error("Compaction failed for docId=$docId: ${e.message}")
            throw e // Let Spring Kafka retry
        }
    }

    fun compactDocument(docId: String, toRev: Int) {
        val meta = metaRepo.findById(docId.toLong()).orElseThrow()
        val fromRev = meta.compactedRevision + 1

        if (fromRev > toRev) return

        // Step 1: Get base text
        val baseText = s3.getPlainText("https://collab-docs-bucket.s3.ap-south-1.amazonaws.com/documents/${docId}.txt")
        var currentText = baseText

        // Step 2: Fetch ops from Cassandra
        val ops = cassRepo.findByDocIdAndRevisionBetween(docId, fromRev, toRev)

        if (ops.isEmpty()) {
            //logger.warn("No operations to compact for docId=$docId")
            return
        }

        // Step 3: Apply deltas
        ops.forEach { op ->
            currentText = applyDelta(currentText, op.delta)
        }

        // Step 4: Upload snapshot to S3 (irreversible!)
        s3.uploadPlainText(docId.toLong(), currentText)

        // Step 5: Update metadata transactionally
        updateMetadata(docId.toLong(), toRev)

        // Step 6: Delete old ops (non-transactional)
        cassRepo.deleteByDocIdAndRevisionLessThanEqual(docId, toRev)

        //logger.info("Compaction complete for docId=$docId, rev $fromRev to $toRev")
    }

    @Transactional
    fun updateMetadata(docId: Long, newRevision: Int) {
        val meta = metaRepo.findById(docId).orElseThrow()
        meta.compactedRevision = newRevision
        metaRepo.save(meta)
    }

}

//private fun partitionFor(docId: String): Int {
//    return Math.abs(docId.hashCode()) % NUM_PARTITIONS
//}

fun applyDelta(base: String, delta: String): String {
    return when {
        delta.contains("-del:") -> {
            val (posStr, lenStr) = delta.split("-del:", limit = 2)
            val pos = posStr.toInt();
            val len = lenStr.toInt()
            base.removeRange(pos, pos + len)
        }

        delta.contains("-rep:") -> {
            val (posStr, lenStr, text) = delta.split("-rep:", ":", limit = 3)
            val pos = posStr.toInt();
            val len = lenStr.toInt()
            base.replaceRange(pos, pos + len, text)
        }

        else -> {
            val (posStr, text) = delta.split(":", limit = 2)
            base.substring(0, posStr.toInt()) + text + base.substring(posStr.toInt())
        }
    }
}


//    @KafkaListener(topics = ["doc-close"], groupId = "compaction-workers")
//    fun onDocClose(trigger: DocumentWebSocketHandler.CompactionTrigger) {
//        val docId = trigger.docId
//        val lockKey = "lock:compact:$docId"
//
//        redisLockService.withLock(lockKey, timeoutMillis = 5000) {
//            val meta = metaRepo.findById(docId.toLong()).orElseThrow()
//
//            val consumer = kafkaConsumerFactory.createConsumer()
//            val partition = TopicPartition("doc-changes", partitionFor(docId))
//            consumer.assign(listOf(partition))
//            consumer.seekToBeginning(listOf(partition))
//
//            val baseText = s3.getPlainText(docId)
//            var currentText = baseText
//            val opsToDelete = mutableListOf<Operation>()
//
//            while (true) {
//                val records = consumer.poll(Duration.ofSeconds(1))
//                if (records.isEmpty) break
//
//                records.records(partition).forEach { record ->
//                    val op = record.value()
//                    if (op.revision > meta.compactedRevision && op.revision <= trigger.latestRevision) {
//                        currentText = applyDelta(currentText, op.delta)
//                        opsToDelete.add(op)
//                    }
//                }
//            }
//
//            // Save new snapshot
//            s3.uploadPlainText(docId.toLong(), currentText)
//
//            // Update compacted revision
//            meta.compactedRevision = trigger.latestRevision
//            metaRepo.save(meta)
//
//            // Cleanup Cassandra
//            cassRepo.deleteByDocIdAndRevisionLessThanEqual(docId, trigger.latestRevision)
//
//            consumer.close()
//        }
//    }



