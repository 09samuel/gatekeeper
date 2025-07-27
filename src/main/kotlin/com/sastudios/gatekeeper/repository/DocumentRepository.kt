package com.sastudios.gatekeeper.repository

import com.sastudios.gatekeeper.entity.Document
import com.sastudios.gatekeeper.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface DocumentRepository : JpaRepository<Document, Long>{
    //fun findByOwnerOrCollaboratorsContaining(owner: User, collaborator: DocumentCollaborator): List<Document>

    @Query("""
    SELECT d FROM Document d 
    WHERE d.owner = :user 
    OR EXISTS (
        SELECT 1 FROM DocumentCollaborator dc 
        WHERE dc.document = d AND dc.user = :user
    )
""")
    fun findAllByOwnerOrCollaborator(@Param("user") user: User): List<Document>


    @Query("""
    UPDATE documents
    SET currentRevision = currentRevision + 1
    WHERE id = :id
    RETURNING currentRevision
""", nativeQuery = true)
    fun incrementAndGetRevision(id: Long): Int


    @Query("SELECT d.currentRevision FROM Document d WHERE d.id = :id")
    fun findLatestRevisionById(@Param("id") id: Long): Int?

}