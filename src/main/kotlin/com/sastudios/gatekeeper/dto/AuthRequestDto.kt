package com.sastudios.gatekeeper.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Pattern

data class AuthRequestDto(
    @field:Email(message = "Invalid email format.")
    val email: String,
    @field:Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{9,}\$",
        message = "Password must be at least 9 characters long and contain at least one digit, uppercase and lowercase character."
    )
    val password: String,
    val name: String
)