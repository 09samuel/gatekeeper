package com.sastudios.gatekeeper.repository

import com.sastudios.gatekeeper.entity.DocumentCollaborator
import org.springframework.data.jpa.repository.JpaRepository

interface DocumentCollaboratorRepository : JpaRepository<DocumentCollaborator, Long> {
    fun findByDocumentIdAndUserId(documentId: Long, userId: Long): DocumentCollaborator?
    fun deleteByDocumentIdAndUserId(documentId: Long, userId: Long)
    fun findAllByDocumentId(documentId: Long): List<DocumentCollaborator>
}

