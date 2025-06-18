package com.sastudios.gatekeeper.security

import com.sastudios.gatekeeper.dto.AddCollaboratorRequestDto
import com.sastudios.gatekeeper.dto.CollaboratorDto
import com.sastudios.gatekeeper.dto.CreateDocumentRequestDto
import com.sastudios.gatekeeper.dto.DocumentResponseDto
import com.sastudios.gatekeeper.dto.UpdateDocumentRequestDto
import com.sastudios.gatekeeper.entity.CollaboratorRole
import com.sastudios.gatekeeper.entity.Document
import com.sastudios.gatekeeper.entity.DocumentCollaborator
import com.sastudios.gatekeeper.entity.User
import com.sastudios.gatekeeper.repository.DocumentCollaboratorRepository
import com.sastudios.gatekeeper.repository.DocumentRepository
import com.sastudios.gatekeeper.repository.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class DocumentService(
    private val jwtService: JwtService,
    @Autowired private val userRepository: UserRepository,
    @Autowired private val documentRepository: DocumentRepository,
    @Autowired private val documentCollaboratorRepository: DocumentCollaboratorRepository
) {

    fun createDocument(request: CreateDocumentRequestDto, token: String): DocumentResponseDto {
        val userIdStr = jwtService.getUserIdFromToken(token)
        val userId = userIdStr.toLongOrNull() ?: throw ResponseStatusException(
            HttpStatus.UNAUTHORIZED,
            "Invalid user ID in token."
        )

        val user = userRepository.findById(userId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        }

        val doc = Document(
            title = request.title,
            content = request.content ?: "",
            owner = user
        )

        val saved = documentRepository.save(doc)

        return DocumentResponseDto(
            id = saved.id,
            title = saved.title,
            content = saved.content,
            ownerId = saved.owner.id,
            createdAt = saved.createdAt,
            collaborators = saved.collaborators.map { it.toDto() },
        )
    }

    fun getUserDocuments(token: String): List<DocumentResponseDto> {
        val user = getUserFromToken(token)
        val documents = documentRepository.findByOwnerOrCollaboratorsContaining(user, user)
        return documents.map { it.toDto() }
    }


    fun getDocumentById(id: Long, token: String): DocumentResponseDto {
        val user = getUserFromToken(token)
        val document = documentRepository.findById(id).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found")
        }

        val isOwner = document.owner.id == user.id
        val isCollaborator = document.collaborators.any { it.user.id == user.id }

        if (!isOwner && !isCollaborator) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "You are neither the owner nor a collaborator")
        }

        return document.toDto()
    }



    fun updateDocument(id: Long, request: UpdateDocumentRequestDto, token: String): DocumentResponseDto {
        val user = getUserFromToken(token)
        val document = documentRepository.findById(id).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found")
        }

        val collab = documentCollaboratorRepository.findByDocumentIdAndUserId(document.id, user.id!!)
        val isEditor = collab?.role == CollaboratorRole.EDITOR

        if (document.owner.id != user.id && !isEditor) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "You don't have edit permission")
        }

        document.title = request.title ?: document.title
        document.content = request.content ?: document.content
        return documentRepository.save(document).toDto()
    }

    fun deleteDocument(id: Long, token: String) {
        val user = getUserFromToken(token)
        val document = documentRepository.findById(id).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found")
        }

        if (document.owner.id != user.id) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Only owner can delete the document")
        }

        documentRepository.delete(document)
    }

    fun getCollaborators(id: Long, token: String): List<CollaboratorDto> {
        val user = getUserFromToken(token)
        val doc = documentRepository.findById(id).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found")
        }

        if (doc.owner.id != user.id && documentCollaboratorRepository.findByDocumentIdAndUserId(id, user.id!!) == null) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied")
        }

        return documentCollaboratorRepository.findAllByDocumentId(id).map { collab ->
            collab.toDto()
        }
    }

    fun addCollaborator(id: Long, request: AddCollaboratorRequestDto, token: String): CollaboratorDto{
        val user = getUserFromToken(token)
        val doc = documentRepository.findById(id).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found")
        }

        if (doc.owner.id != user.id) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Only owner can add collaborators")
        }

        val newUser = userRepository.findById(request.userId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        }

        val role = try {
            CollaboratorRole.valueOf(request.role.toString().uppercase())
        } catch (e: Exception) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role")
        }

        // Check for existing collaboration
        val existing = documentCollaboratorRepository.findByDocumentIdAndUserId(id, request.userId)
        if (existing != null) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "User is already a collaborator")
        }

        // Save explicitly via the collaborator repository
        val collaborator = DocumentCollaborator(
            document = doc,
            user = newUser,
            role = role
        )

        doc.collaborators.add(collaborator)
        documentRepository.save(doc)

        //documentCollaboratorRepository.save(collaborator)

        return collaborator.toDto()
    }

    fun removeCollaborator(id: Long, userId: Long, token: String) {
        val user = getUserFromToken(token)
        val doc = documentRepository.findById(id).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found")
        }

        if (doc.owner.id != user.id) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Only owner can remove collaborators")
        }

        val toRemove = doc.collaborators.find { it.user.id == userId }
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Collaborator not found")

        doc.collaborators.remove(toRemove)
        documentRepository.save(doc)
    }


    private fun getUserFromToken(token: String): User {
        val userId = jwtService.getUserIdFromToken(token).toLongOrNull()
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token")
        return userRepository.findById(userId).orElseThrow {
            ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found")
        }
    }

}



private fun Document.toDto(): DocumentResponseDto {
    return DocumentResponseDto(
        id = this.id,
        title = this.title,
        content = this.content,
        ownerId = this.owner.id ?: throw IllegalStateException("Owner ID is null"),
        collaborators = this.collaborators.map { it.toDto() },
        createdAt = this.createdAt
    )
}


//fun User.toDtoWithRole(role: CollaboratorRole): UserResponseDto {
//    return UserResponseDto(
//        id = this.id ?: throw IllegalStateException("User ID is null"),
//        email = this.email,
//        name = this.name,
//        role = role.name
//    )
//}

fun DocumentCollaborator.toDto(): CollaboratorDto {
    return CollaboratorDto(
        userId = this.user.id ?: throw IllegalStateException("User ID is null"),
        role = this.role.name
    )
}
