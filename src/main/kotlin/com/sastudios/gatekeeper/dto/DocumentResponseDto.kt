package com.sastudios.gatekeeper.dto

import java.time.Instant

data class DocumentResponseDto(
    val id: Long,
    val title: String,
    val content: String,
    val ownerId: Long?,
    val createdAt: Instant,
    val collaborators: List<CollaboratorDto>
)