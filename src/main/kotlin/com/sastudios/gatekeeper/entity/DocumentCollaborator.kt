package com.sastudios.gatekeeper.entity

import jakarta.persistence.*

@Entity
@Table(name = "document_collaborators")
data class DocumentCollaborator(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    val document: Document,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val user: User,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val role: CollaboratorRole = CollaboratorRole.VIEWER
)

enum class CollaboratorRole {
    VIEWER,
    EDITOR
}

