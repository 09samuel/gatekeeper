package com.sastudios.gatekeeper.controllers


import com.sastudios.gatekeeper.dto.AuthRequestDto
import com.sastudios.gatekeeper.dto.UserResponseDto
import com.sastudios.gatekeeper.security.AuthService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/gatekeeper/auth")
class AuthController(
    private val authService: AuthService
) {
    data class RefreshRequest(
        val refreshToken: String
    )

    @PostMapping("/register")
    fun register(
        @Valid @RequestBody body: AuthRequestDto
    ): UserResponseDto {
        val registeredUser = authService.register(body.email, body.password)
        return UserResponseDto(registeredUser.id, registeredUser.email)
    }

    @PostMapping("/login")
    fun login(
        @RequestBody body: AuthRequestDto
    ): AuthService.TokenPair {
        return authService.login(body.email, body.password)
    }

    @PostMapping("/refresh")
    fun refresh(
        @RequestBody body: RefreshRequest
    ): AuthService.TokenPair {
        return authService.refresh(body.refreshToken)
    }

    @PostMapping("/logout")
    fun logout(
        @RequestBody body: RefreshRequest
    ) {
        return authService.logout(body.refreshToken)
    }
}