package com.sastudios.gatekeeper.service

import com.sastudios.gatekeeper.dto.AddCollaboratorRequestDto
import com.sastudios.gatekeeper.dto.CollaboratorDto
import com.sastudios.gatekeeper.dto.CreateDocumentRequestDto
import com.sastudios.gatekeeper.dto.DocumentResponseDto
import com.sastudios.gatekeeper.dto.UpdateDocumentRequestDto
import com.sastudios.gatekeeper.entity.CassandraOperation

import com.sastudios.gatekeeper.entity.CollaboratorRole
import com.sastudios.gatekeeper.entity.Document
import com.sastudios.gatekeeper.entity.DocumentCollaborator
import com.sastudios.gatekeeper.entity.User
import com.sastudios.gatekeeper.model.Operation
import com.sastudios.gatekeeper.repository.CassandraOperationRepository

import com.sastudios.gatekeeper.repository.DocumentCollaboratorRepository
import com.sastudios.gatekeeper.repository.DocumentRepository
import com.sastudios.gatekeeper.repository.UserRepository
import com.sastudios.gatekeeper.security.JwtService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class DocumentService(
    private val jwtService: JwtService,
    private val s3Service: S3Service,
    @Autowired private val userRepository: UserRepository,
    @Autowired private val documentRepository: DocumentRepository,
    @Autowired private val documentCollaboratorRepository: DocumentCollaboratorRepository,
    private val cassRepo: CassandraOperationRepository,
    private val transformer: SimpleTextTransformer
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

        // Step 1: Save doc without content URL
        val tempDoc = Document(
            title = request.title,
            contentUrl = "", // Will be set after S3 upload
            owner = user
        )
        val saved = documentRepository.save(tempDoc)

        // Step 2: Upload content to S3 and get URL
        val content = request.content ?: ""
        val contentUrl = s3Service.uploadPlainText(saved.id, content)

        // Step 3: Update doc with S3 URL
        saved.contentUrl = contentUrl
        val finalSaved = documentRepository.save(saved)

        return DocumentResponseDto(
            id = finalSaved.id,
            title = finalSaved.title,
            contentUrl = finalSaved.contentUrl,
            ownerId = finalSaved.owner.id,
            createdAt = finalSaved.createdAt,
            collaborators = finalSaved.collaborators.map { it.toDto() }
        )
    }

    fun getUserDocuments(token: String): List<DocumentResponseDto> {
        val user = getUserFromToken(token)
        val documents = documentRepository.findAllByOwnerOrCollaborator(user)
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

        val content = s3Service.getPlainText(document.contentUrl)

        return document.toDto().copy(content = content)

    }

//    fun getDocumentById(id: Long, token: String): DocumentResponseDto {
//        val user = getUserFromToken(token)
//        val document = documentRepository.findById(id).orElseThrow {
//            ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found")
//        }
//
//        val isOwner = document.owner.id == user.id
//        val isCollaborator = document.collaborators.any { it.user.id == user.id }
//
//        if (!isOwner && !isCollaborator) {
//            throw ResponseStatusException(HttpStatus.FORBIDDEN, "You are neither the owner nor a collaborator")
//        }
//
//        val content = s3Service.getPlainText(document.contentUrl)
//
//        val history = cassRepo.findByDocIdAndRevisionGreaterThan(id.toString(), document.compactedRevision)
//            .map { Operation(it.docId, it.userId, it.baseRevision, it.revision, it.delta) }
//
//        return DocumentResponseDto(
//            id = document.id,
//            title = document.title,
//            content = content,
//            ownerId = document.owner.id!!,
//            collaborators = document.collaborators.map { it.toDto() },
//            createdAt = document.createdAt,
//            operations = history,
//        )
//    }



//    fun getState(docId: String): Pair<String, List<Operation>> {
//        val docId = docId.toLongOrNull() ?: throw IllegalArgumentException("Invalid docId")
//        val meta = documentRepository.findById(docId).orElseThrow()
//
//        // fetch from S3 + pending ops
//        val history = cassRepo.findByDocIdAndRevisionGreaterThan(docId.toString(), meta.compactedRevision)
//            .map { Operation(it.docId, it.userId, it.baseRevision, it.revision, it.delta) }
//        return Pair("S3-content-placeholder", history)
//    }



//    fun updateDocument(id: Long, request: UpdateDocumentRequestDto, token: String): DocumentResponseDto {
//        val user = getUserFromToken(token)
//        val document = documentRepository.findById(id).orElseThrow {
//            ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found")
//        }
//
//        val collab = documentCollaboratorRepository.findByDocumentIdAndUserId(document.id, user.id!!)
//        val isEditor = collab?.role == CollaboratorRole.EDITOR
//
//        if (document.owner.id != user.id && !isEditor) {
//            throw ResponseStatusException(HttpStatus.FORBIDDEN, "You don't have edit permission")
//        }
//
//        document.title = request.title ?: document.title
//        document.content = request.content ?: document.content
//        return documentRepository.save(document).toDto()
//    }

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

//    @Transactional
//    fun applyOperation(op: Operation): Operation {
//        val docId = op.docId.toLongOrNull() ?: throw IllegalArgumentException("Invalid docId")
//        val meta = documentRepository.findById(docId).orElseThrow()
//
//        val history = cassRepo.findByDocIdAndRevisionGreaterThan(op.docId, op.baseRevision)
//            .map { Operation(it.docId, it.userId, it.baseRevision, it.revision, it.delta) }
//        val transformed = transformer.transform(op, history)
//        val cass = CassandraOperation(
//            transformed.docId, transformed.revision, transformed.userId,
//            transformed.baseRevision, transformed.delta
//        )
//        cassRepo.save(cass)
//        meta.currentRevision = transformed.revision
//        documentRepository.save(meta)
//        return transformed
//    }


}

fun Document.toDto(): DocumentResponseDto {
    return DocumentResponseDto(
        id = this.id,
        title = this.title,
        contentUrl = this.contentUrl,
        ownerId = this.owner.id ?: throw IllegalStateException("Owner ID is null"),
        collaborators = this.collaborators.map {
            CollaboratorDto(
                userId = it.user.id ?: throw IllegalStateException("User ID is null"),
                role = it.role.name
            )
        },
        createdAt = this.createdAt
    )
}

//private fun Document.toDto(): DocumentResponseDto {
//    return DocumentResponseDto(
//        id = this.id,
//        title = this.title,
//        content = this.content,
//        ownerId = this.owner.id ?: throw IllegalStateException("Owner ID is null"),
//        collaborators = this.collaborators.map { it.toDto() },
//        createdAt = this.createdAt
//    )
//}

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
