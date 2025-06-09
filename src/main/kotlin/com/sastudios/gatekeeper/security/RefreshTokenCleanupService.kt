package com.sastudios.gatekeeper.security

import com.sastudios.gatekeeper.repository.RefreshTokenRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class RefreshTokenCleanupService(
    private val refreshTokenRepository: RefreshTokenRepository
) {

    @Scheduled(cron = "0 0 0 * * *")
    fun deleteExpiredTokens() {
        val now = Instant.now()
        refreshTokenRepository.deleteByExpiresAtBefore(now)
    }
}