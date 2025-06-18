package com.sastudios.gatekeeper.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*

@Entity
@Table(name = "document_collaborators")
data class DocumentCollaborator(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    @JsonIgnore
    val document: Document,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val user: User,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val role: CollaboratorRole = CollaboratorRole.VIEWER
){
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DocumentCollaborator) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

enum class CollaboratorRole {
    VIEWER,
    EDITOR
}

