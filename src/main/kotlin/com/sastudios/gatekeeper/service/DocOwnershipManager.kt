//package com.sastudios.gatekeeper.service
//
//import org.slf4j.LoggerFactory
//import org.springframework.beans.factory.annotation.Value
//import org.springframework.data.redis.core.StringRedisTemplate
//import org.springframework.stereotype.Component
//import java.time.Duration
//
//@Component
//class DocOwnershipManager(
//    private val redis: StringRedisTemplate,
//    @Value("\${ws.instance.id}") private val instanceId: String,
//    private val deadInstanceScanner: DeadInstanceScanner,
//    private val heartbeat: HeartbeatService,
//) {
//    private val logger = LoggerFactory.getLogger(this::class.java)
//
////    fun tryOwn(docId: String): Boolean {
////        val key = "doc:$docId"
////        val success = redis.opsForValue().setIfAbsent(key, instanceId, Duration.ofDays(1) )
////        redis.opsForValue().setIfAbsent(
////            "doc:$docId:$instanceId",
////            instanceId,
////            Duration.ofMillis(300000)
////        )
////        return success ?: (redis.opsForValue().get(key) == instanceId)
////    }
//
//    fun tryOwn(docId: String): Boolean {
//        // Fast path: Check if we're the responsible instance first
//        val responsibleInstance = deadInstanceScanner.getResponsibleInstance(docId, heartbeat.getLiveInstances())
//        if (responsibleInstance != instanceId) return false
//
//        // Slow path: Atomic Redis claim
//        return redis.opsForValue().setIfAbsent(
//            "doc:$docId:$instanceId",
//            instanceId,
//            Duration.ofMillis(300000)
//        ) ?: (redis.opsForValue().get("doc:owner:$docId") == instanceId)
//    }
//
//    fun isOwner(docId: String): Boolean {
//        return redis.opsForValue().get("doc:$docId") == instanceId
//    }
//
//    fun release(docId: String) {
//        logger.info("🕓 release reached")
//        val docKey = "doc:$docId"
//        val currentOwner = redis.opsForValue().get(docKey)
//
//        if (currentOwner == instanceId) {
//            // Only set expiration if this instance still owns it
//            //redis.expire(docKey, Duration.ofSeconds(30))
//            redis.delete(docKey)
//            logger.info("🕓 Soft-released ownership of $docKey with 30s TTL")
//        } else {
//            logger.warn("⚠️ Skipped releasing $docKey — not owned by this instance ($instanceId)")
//        }
//    }
//
//}
