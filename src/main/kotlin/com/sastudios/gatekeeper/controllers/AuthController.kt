package com.sastudios.gatekeeper.controllers


import com.sastudios.gatekeeper.dto.LoginRequestDto
import com.sastudios.gatekeeper.dto.RegisterRequestDto
import com.sastudios.gatekeeper.dto.RefreshRequestDto
import com.sastudios.gatekeeper.dto.UserResponseDto
import com.sastudios.gatekeeper.repository.UserRepository
import com.sastudios.gatekeeper.security.AuthService
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/gatekeeper/auth")
class AuthController(
    private val authService: AuthService,
    @Autowired private val userRepository: UserRepository
) {

    @PostMapping("/register")
    fun register(
        @Valid @RequestBody body: RegisterRequestDto
    ): UserResponseDto {
        val registeredUser = authService.register(body.email, body.password, body.name)
        return UserResponseDto(registeredUser.id, registeredUser.email, registeredUser.name)
    }

    @PostMapping("/login")
    fun login(
        @RequestBody body: LoginRequestDto
    ): ResponseEntity<AuthService.TokenPair> {
        val tokenPair  = authService.login(body.email, body.password)
        return ResponseEntity.ok(tokenPair)

    }

    @PostMapping("/refresh")
    fun refresh(
        @RequestBody body: RefreshRequestDto
    ): AuthService.TokenPair {
        return authService.refresh(body.refreshToken)
    }

//    @PostMapping("/logout")
//    fun logout(
//        @RequestBody body: RefreshRequestDto
//    ) {
//        return authService.logout(body.refreshToken)
//    }

//    @PostMapping("/logout")
//    fun logout(
//        @RequestHeader("Authorization") token: String
//    ) {
//        return authService.logout(token)
//    }

    @GetMapping("/me")
    fun me() : UserResponseDto {
        val id = SecurityContextHolder.getContext().authentication.principal as String

        val userId = id.toLongOrNull() ?: throw ResponseStatusException(
            HttpStatus.UNAUTHORIZED, "Invalid token payload"
        )
        val user = userRepository.findById(userId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "User not found") }

        return UserResponseDto(
            id = user.id ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Invalid user ID"),
            email = user.email,
            name = user.name
        )
    }
}