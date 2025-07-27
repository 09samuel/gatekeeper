package com.sastudios.gatekeeper.service

import kotlinx.coroutines.Dispatchers
import org.springframework.stereotype.Service


import org.redisson.api.RedissonClient
import org.redisson.api.RLock
import java.util.concurrent.TimeUnit

@Service
class RedissonLockService(private val redissonClient: RedissonClient) {

//    fun <T> withLock(
//        lockKey: String,
//        waitTime: Long = 5,
//        action: () -> T
//    ): T {
//        val lock: RLock = redissonClient.getLock(lockKey)
//        val acquired = lock.tryLock(waitTime, TimeUnit.SECONDS)
//        if (!acquired) throw IllegalStateException("Could not acquire lock on $lockKey")
//
//        return try {
//            action()
//        } finally {
//            if (lock.isHeldByCurrentThread) {
//                lock.unlock()
//            }
//        }
//    }

     suspend fun <T> withLock(key: String, block: suspend () -> T): T {
        val lock = redissonClient.getLock(key)

        return kotlinx.coroutines.withContext(Dispatchers.IO) {
            val acquired = lock.tryLock(5, 10, TimeUnit.SECONDS)
            if (!acquired) throw IllegalStateException("Failed to acquire lock for $key")

            try {
                block()
            } finally {
                if (lock.isHeldByCurrentThread) {
                    lock.unlock()
                }
            }
        }
    }
}
