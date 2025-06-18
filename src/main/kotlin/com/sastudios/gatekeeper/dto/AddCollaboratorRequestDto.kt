package com.sastudios.gatekeeper.dto

import com.sastudios.gatekeeper.entity.CollaboratorRole

data class AddCollaboratorRequestDto(
    val userId: Long,
    val role: CollaboratorRole // EDITOR, VIEWER
)

