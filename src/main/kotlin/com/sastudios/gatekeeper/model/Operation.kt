package com.sastudios.gatekeeper.model

data class Operation(
    val docId: String,
    val userId: String,
    val baseRevision: Int,
    val revision: Int,
    val delta: String,
    val senderConnId: String? = null
)
