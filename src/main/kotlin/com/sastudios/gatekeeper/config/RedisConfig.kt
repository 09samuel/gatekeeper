//package com.sastudios.gatekeeper.config
//
//import org.slf4j.LoggerFactory
//import org.springframework.context.annotation.Bean
//import org.springframework.context.annotation.Configuration
//import org.springframework.data.redis.connection.RedisConnectionFactory
//import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
//import org.springframework.data.redis.core.StringRedisTemplate
//import org.springframework.data.redis.core.script.DefaultRedisScript
//import org.springframework.data.redis.core.script.RedisScript
//
//@Configuration
//class RedisConfig {
//    @Bean
//    fun reclaimScript(): RedisScript<Boolean> {
//         val logger = LoggerFactory.getLogger(this::class.java)
//        logger.warn("reclaim script")
//        val script =
//            """
//            if redis.call('GET', KEYS[1]) == ARGV[1] then
//                return redis.call('SET', KEYS[1], ARGV[2], 'EX', 86400) == 'OK'
//            end
//            return false
//            """.trimIndent()
//        return DefaultRedisScript(script, Boolean::class.java)
//    }
//
//}
