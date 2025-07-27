package com.sastudios.gatekeeper.service

import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ScanOptions
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.http.ResponseEntity
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

//
//import org.slf4j.LoggerFactory
//import org.springframework.beans.factory.annotation.Value
//import org.springframework.data.redis.core.ScanOptions
//
//import org.springframework.data.redis.core.StringRedisTemplate
//import org.springframework.data.redis.core.script.RedisScript
//import org.springframework.scheduling.annotation.Scheduled
//import org.springframework.stereotype.Component
//
//
//@Component
//class DeadInstanceScanner(
//    private val redis: StringRedisTemplate,
//    @Value("\${ws.instance.id}") private val myId: String,
//    private val heartbeat: InstanceHeartbeat,
//    private val reclaimScript: RedisScript<Boolean>
//) {
//    private val logger = LoggerFactory.getLogger(this::class.java)
//
//    @Scheduled(fixedDelay = 15000)
//    fun scanAndReassign() {
//        logger.info("🔍 Starting dead instance scan...")
//
//        try {
//            val keys = scanKeys("doc:*")
//
//
//            logger.debug("📄 Found ${keys.size} documents to check")
//            var reclaimedCount = 0
//
//            for (docKey in keys) {
//                val docId = docKey.removePrefix("doc:")
//                val owner = redis.opsForValue().get(docKey)
//
//                logger.debug("🔎 Checking doc:$docId owned by $owner")
//
//                if (owner != null && !heartbeat.isInstanceAlive(owner)) {
//                    logger.warn("❗ Dead instance detected: $owner for doc:$docId")
//                    if (tryReclaim(docKey, owner, myId)) {
//                        reclaimedCount++
//                        logger.info("✅ Successfully reclaimed doc:$docId by $myId")
//                    } else {
//                        val currentOwner = redis.opsForValue().get(docKey)
//                        logger.warn("❌ Reclaim failed for doc:$docId. Tried claiming from $owner → but it's now owned by: $currentOwner")
//                    }
//                }
//            }
//
//            logger.info("✅ Scan complete. Reclaimed $reclaimedCount document(s).")
//        } catch (e: Exception) {
//            logger.error("🚨 Failed during dead instance scan", e)
//        }
//    }
//
//    private fun tryReclaim(docKey: String, deadOwner: String, myId: String): Boolean {
//        return redis.execute(
//            reclaimScript,
//            listOf(docKey),
//            deadOwner,
//            myId
//        )
//    }
//
//    private fun scanKeys(matchPattern: String = "doc:*"): List<String> {
//        val keys = mutableListOf<String>()
//        var cursor = ScanOptions.NONE
//
//        do {
//            val scanCursor = redis.scan(
//                ScanOptions.scanOptions().match(matchPattern).count(100).build()
//            )
//            while (scanCursor.hasNext()) {
//                keys.add(scanCursor.next())
//            }
//            cursor = scanCursor.cursor
//        } while (!cursor.isFinished)
//
//        return keys
//    }
//
//    private fun scanKeys(matchPattern: String = "doc:*"): List<String> {
//        val keys = mutableListOf<String>()
//        val redisTemplate: StringRedisTemplate = // Autowire or inject this
//
//        // Create scan options
//        val scanOptions = ScanOptions.scanOptions()
//            .match(matchPattern)
//            .count(100) // Approximate batch size
//            .build()
//
//        // Use RedisTemplate's scan method
//        val cursor = redisTemplate.scan(scanOptions)
//
//        while (cursor.hasNext()) {
//            keys.add(cursor.next())
//        }
//
//        // Important: Close the cursor to avoid resource leaks!
//        cursor.close()
//
//        return keys
//    }
//
//}
//
//
//@RestController
//@EnableScheduling
//class DeadInstanceScanner(
//    private val redis: RedisTemplate<String, String>,
//    private val heartbeat: HeartbeatService,
//    private val reclaimProps: ReclaimerProperties
//) {
//    private val logger = LoggerFactory.getLogger(this::class.java)
//    private val myId = UUID.randomUUID().toString() // Unique instance ID
//    private val backoff = AtomicLong(reclaimProps.initialDelay)
//
//    // Lua script for atomic ownership transfer
//    private val reclaimScript = RedisScript.of(
//        """
//        local key = KEYS[1]
//        local expectedOwner = ARGV[1]
//        local newOwner = ARGV[2]
//        local expiry = tonumber(ARGV[3])
//
//        local currentOwner = redis.call('GET', key)
//        if currentOwner == expectedOwner then
//            redis.call('SET', key, newOwner)
//            redis.call('EXPIREAT', key, expiry)
//            return true
//        end
//        return false
//        """,
//        Boolean::class.java
//    )
//
//    @Scheduled(fixedDelayString = "#{@deadInstanceScanner.currentDelay}")
//    fun scanAndReassign() {
//        try {
//            val deadInstances = heartbeat.getRecentlyDeadInstances(reclaimProps.deadInstanceThreshold)
//            if (deadInstances.isEmpty()) {
//                resetBackoff()
//                return
//            }
//
//            val liveInstances = heartbeat.getLiveInstances().sorted()
//            if (liveInstances.size == 1 && liveInstances[0] == myId) {
//                logger.warn("Single instance cluster - no reclaims needed")
//                return
//            }
//
//            var reclaimedCount = 0
//            deadInstances.forEach { deadInstance ->
//                redis.scan("doc:*:$deadInstance").forEach { key ->
//                    val docId = key.removePrefix("doc:").substringBeforeLast(":")
//                    if (getResponsibleInstance(docId, liveInstances) == myId) {
//                        if (tryReclaim(key, deadInstance)) {
//                            reclaimedCount++
//                        }
//                    }
//                }
//            }
//
//            logger.info("Reclaimed $reclaimedCount documents")
//            resetBackoff()
//        } catch (e: Exception) {
//            logger.error("Reclaim failed", e)
//            increaseBackoff()
//        }
//    }
//
//    // Helper functions
//    fun getResponsibleInstance(docId: String, liveInstances: List<String>): String {
//        val shard = docId.hashCode() % liveInstances.size
//        return liveInstances[shard]
//    }
//
//    private fun tryReclaim(key: String, deadOwner: String): Boolean {
//        return redis.execute(
//            reclaimScript,
//            listOf(key),
//            deadOwner,
//            myId,
//            Instant.now().plusMillis(reclaimProps.ownershipDuration).epochSecond.toString()
//        ) ?: false
//    }
//
//    fun currentDelay(): Long = backoff.get()
//
//    private fun resetBackoff() {
//        backoff.set(reclaimProps.initialDelay)
//    }
//
//    private fun increaseBackoff() {
//        backoff.updateAndGet { curr ->
//            minOf(curr * 2, reclaimProps.maxDelay)
//        }
//    }
//}
//
//// Configuration
//@ConfigurationProperties(prefix = "reclaimer")
//data class ReclaimerProperties(
//    val initialDelay: Long = 15000,
//    val maxDelay: Long = 60000,
//    val deadInstanceThreshold: Long = 30000,
//    val ownershipDuration: Long = 300000
//)
//
//// Heartbeat Service Interface
//interface HeartbeatService {
//    fun getLiveInstances(): List<String>
//    fun getRecentlyDeadInstances(thresholdMs: Long): List<String>
//}
//
//// Redis scan extension
//fun RedisTemplate<String, String>.scan(pattern: String): Sequence<String> = sequence {
//    val connection = requireNotNull(connectionFactory?.connection)
//    val scanOptions = ScanOptions.scanOptions().match(pattern).count(100).build()
//    connection.scan(scanOptions).forEach { yield(String(it)) }
//    connection.close()
//}