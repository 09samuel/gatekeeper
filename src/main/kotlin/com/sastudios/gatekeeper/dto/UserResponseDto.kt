package com.sastudios.gatekeeper.dto

data class UserResponseDto(
    val id: Long?,
    val email: String,
    val name: String,
    val role: String? = null
)
