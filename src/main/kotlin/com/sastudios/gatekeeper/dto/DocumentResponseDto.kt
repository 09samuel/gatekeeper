package com.sastudios.gatekeeper.dto

import com.sastudios.gatekeeper.model.Operation
import java.time.Instant

data class DocumentResponseDto(
    val id: Long,
    val title: String,
    val contentUrl: String? = null,
    val content: String? = null,
    val ownerId: Long?,
    val createdAt: Instant,
    val collaborators: List<CollaboratorDto>,
    val operations: List<Operation> = emptyList()
)