package com.sastudios.gatekeeper.controllers

import com.sastudios.gatekeeper.dto.AddCollaboratorRequestDto
import com.sastudios.gatekeeper.dto.CollaboratorDto
import com.sastudios.gatekeeper.dto.CreateDocumentRequestDto
import com.sastudios.gatekeeper.dto.DocumentResponseDto
import com.sastudios.gatekeeper.dto.UpdateDocumentRequestDto
import com.sastudios.gatekeeper.security.DocumentService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/gatekeeper/document")
class DocumentController (private val documentService: DocumentService){

    @PostMapping
    fun createDoc(
        @RequestBody request: CreateDocumentRequestDto,
        @RequestHeader("Authorization") token: String
    ): DocumentResponseDto = documentService.createDocument(request, token)


    @GetMapping
    fun listDocs(@RequestHeader("Authorization") token: String): List<DocumentResponseDto> =
        documentService.getUserDocuments(token)

    @GetMapping("/{id}")
    fun getDoc(
        @PathVariable id: Long,
        @RequestHeader("Authorization") token: String
    ): DocumentResponseDto = documentService.getDocumentById(id, token)

    @PutMapping("/{id}")
    fun updateDoc(
        @PathVariable id: Long,
        @RequestBody request: UpdateDocumentRequestDto,
        @RequestHeader("Authorization") token: String
    ): DocumentResponseDto = documentService.updateDocument(id, request, token)

    @DeleteMapping("/{id}")
    fun deleteDoc(
        @PathVariable id: Long,
        @RequestHeader("Authorization") token: String
    ): ResponseEntity<Void> {
        documentService.deleteDocument(id, token)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/{id}/collaborators")
    fun getCollaborators(
        @PathVariable id: Long,
        @RequestHeader("Authorization") token: String
    ): List<CollaboratorDto> = documentService.getCollaborators(id, token)

    @PostMapping("/{id}/collaborators")
    fun addCollaborator(
        @PathVariable id: Long,
        @RequestBody request: AddCollaboratorRequestDto,
        @RequestHeader("Authorization") token: String
    ): CollaboratorDto = documentService.addCollaborator(id, request, token)

    @DeleteMapping("/{id}/collaborators/{userId}")
    fun removeCollaborator(
        @PathVariable id: Long,
        @PathVariable userId: Long,
        @RequestHeader("Authorization") token: String
    ) = documentService.removeCollaborator(id, userId, token)
}