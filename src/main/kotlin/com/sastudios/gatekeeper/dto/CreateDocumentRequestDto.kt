package com.sastudios.gatekeeper.dto

data class CreateDocumentRequestDto(
    val title: String,
    val content: String? = ""
)