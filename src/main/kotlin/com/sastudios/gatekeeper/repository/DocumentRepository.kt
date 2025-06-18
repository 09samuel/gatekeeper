package com.sastudios.gatekeeper.repository

import com.sastudios.gatekeeper.entity.Document
import com.sastudios.gatekeeper.entity.User
import org.springframework.data.jpa.repository.JpaRepository

interface DocumentRepository : JpaRepository<Document, Long>{
    fun findByOwnerOrCollaboratorsContaining(owner: User, collaborator: User): List<Document>
}