package com.sastudios.gatekeeper.security



import com.sastudios.gatekeeper.entity.RefreshToken
import com.sastudios.gatekeeper.entity.User
import com.sastudios.gatekeeper.repository.RefreshTokenRepository
import com.sastudios.gatekeeper.repository.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.security.MessageDigest
import java.time.Instant
import java.util.Base64

@Service
class AuthService(
    private val jwtService: JwtService,
    @Autowired private val userRepository: UserRepository,
    private val hashEncoder: HashEncoder,
    @Autowired private val refreshTokenRepository: RefreshTokenRepository
) {
    data class TokenPair(
        val accessToken: String,
        val refreshToken: String
    )

    fun register(email: String, password: String): User {
        val user = userRepository.findByEmail(email.trim())
        if (user != null) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "A user with that email already exists.")
        }
        return userRepository.save(
            User(
                email = email,
                hashedPassword = hashEncoder.encode(password),
            )
        )
    }

    fun login(email: String, password: String): TokenPair {
        val user = userRepository.findByEmail(email)
            ?: throw BadCredentialsException("Invalid credentials.")

        if (!hashEncoder.matches(password, user.hashedPassword)) {
            throw BadCredentialsException("Invalid credentials.")
        }

        val userIdStr = user.id?.toString()
            ?: throw IllegalStateException("User ID must not be null.")

        val newAccessToken = jwtService.generateAccessToken(userIdStr)
        val newRefreshToken = jwtService.generateRefreshToken(userIdStr)

        storeRefreshToken(user, newRefreshToken)

        return TokenPair(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken
        )
    }

    @Transactional
    fun refresh(refreshToken: String): TokenPair {
        if (!jwtService.validateRefreshToken(refreshToken)) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token.")
        }

        val userIdStr = jwtService.getUserIdFromToken(refreshToken)
        val userId = userIdStr.toLongOrNull() ?: throw ResponseStatusException(
            HttpStatus.UNAUTHORIZED,
            "Invalid user ID in token."
        )

        val user = userRepository.findById(userId).orElseThrow {
            ResponseStatusException(HttpStatusCode.valueOf(401), "Invalid refresh token.")
        }

        val hashed = hashToken(refreshToken)
        val storedToken = refreshTokenRepository.findByUserIdAndHashedToken(userId, hashed)
            ?: throw ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Refresh token not recognized (maybe used or expired?)"
            )

        if (storedToken.expiresAt.isBefore(Instant.now())) {
            throw ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Refresh token has expired"
            )
        }

        refreshTokenRepository.deleteByUserIdAndHashedToken(userId, hashed)

        val newAccessToken = jwtService.generateAccessToken(userIdStr)
        val newRefreshToken = jwtService.generateRefreshToken(userIdStr)

        storeRefreshToken(user, newRefreshToken)

        return TokenPair(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken
        )
    }

    @Transactional
    fun logout(refreshToken: String) {
        if (!jwtService.validateRefreshToken(refreshToken)) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token.")
        }

        val userIdStr = jwtService.getUserIdFromToken(refreshToken)
        val userId = userIdStr.toLongOrNull() ?: throw ResponseStatusException(
            HttpStatus.UNAUTHORIZED,
            "Invalid user ID in token."
        )

        val hashed = hashToken(refreshToken)
        val storedToken = refreshTokenRepository.findByUserIdAndHashedToken(userId, hashed)
            ?: throw ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Refresh token not recognized (maybe used or expired?)"
            )

        if (storedToken.expiresAt.isBefore(Instant.now())) {
            throw ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Refresh token has expired"
            )
        }

        refreshTokenRepository.deleteByUserIdAndHashedToken(userId, hashed)
    }


    private fun storeRefreshToken(user: User, rawRefreshToken: String) {
        val hashed = hashToken(rawRefreshToken)
        val expiryMs = jwtService.refreshTokenValidityMs
        val expiresAt = Instant.now().plusMillis(expiryMs)

        refreshTokenRepository.save(
            RefreshToken(
                user = user,
                expiresAt = expiresAt,
                hashedToken = hashed,
            )
        )
    }

    private fun hashToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(token.encodeToByteArray())
        return Base64.getEncoder().encodeToString(hashBytes)
    }
}
