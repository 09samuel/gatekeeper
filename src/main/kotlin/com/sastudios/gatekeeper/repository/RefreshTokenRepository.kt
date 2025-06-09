package com.sastudios.gatekeeper.repository

import com.sastudios.gatekeeper.entity.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant

interface RefreshTokenRepository: JpaRepository<RefreshToken, Long> {
    fun findByUserIdAndHashedToken(userId: Long, hashedToken: String): RefreshToken?
    fun deleteByUserIdAndHashedToken(userId: Long, hashedToken: String)
    fun deleteByExpiresAtBefore(time: Instant): Int
}
