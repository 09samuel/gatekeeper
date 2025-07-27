//package com.sastudios.gatekeeper.service
//
//import org.slf4j.LoggerFactory
//import org.springframework.beans.factory.annotation.Value
//import org.springframework.data.redis.core.StringRedisTemplate
//import org.springframework.scheduling.annotation.Scheduled
//import org.springframework.stereotype.Component
//import java.time.Duration
//
////@Component
////class InstanceHeartbeat(
////    private val redis: StringRedisTemplate,
////    @Value("\${ws.instance.id}") private val instanceId: String
////) {
////
////    @Scheduled(fixedDelay = 5000)
////    fun sendHeartbeat() {
////        redis.opsForValue().set("instance:$instanceId", System.currentTimeMillis().toString(), Duration.ofSeconds(10))
////    }
////
////    fun isInstanceAlive(otherId: String): Boolean {
////        val ts = redis.opsForValue().get("instance:$otherId")?.toLongOrNull() ?: return false
////        return System.currentTimeMillis() - ts < 10_000
////    }
////}
//
//@Component
//class InstanceHeartbeat(
//    private val redis: StringRedisTemplate,
//    @Value("\${ws.instance.id}") private val instanceId: String
//) {
//    private val logger = LoggerFactory.getLogger(this::class.java)
//
//    @Scheduled(fixedDelay = 5000)
//    fun sendHeartbeat() {
//        try {
//            val timestamp = System.currentTimeMillis()
//            redis.opsForValue().set(
//                "instance:$instanceId",
//                timestamp.toString(),
//                Duration.ofSeconds(10)
//            )
//            logger.info("Heartbeat sent for instance $instanceId at $timestamp")
//        } catch (e: Exception) {
//            logger.error("Failed to send heartbeat", e)
//        }
//    }
//
//    fun isInstanceAlive(otherId: String): Boolean {
//        return try {
//            val ts = redis.opsForValue().get("instance:$otherId")?.toLongOrNull() ?: run {
//                logger.debug("Instance $otherId has no heartbeat record")
//                return false
//            }
//            val isAlive = System.currentTimeMillis() - ts < 10_000
//            if (!isAlive) {
//                logger.info("Instance $otherId appears dead (last heartbeat: ${System.currentTimeMillis() - ts}ms ago)")
//            }
//            isAlive
//        } catch (e: Exception) {
//            logger.error("Failed to check instance liveness for $otherId", e)
//            false
//        }
//    }
//}
