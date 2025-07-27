package com.sastudios.gatekeeper.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "documents")
data class Document(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    var title: String = "",

    @Column(name = "content_url", nullable = false)
    var contentUrl: String = "", // S3 URL

    @ManyToOne
    @JoinColumn(name = "owner_id")
    val owner: User,

    @OneToMany(mappedBy = "document", cascade = [CascadeType.ALL], orphanRemoval = true)
    val collaborators: MutableSet<DocumentCollaborator> = mutableSetOf(),

    val createdAt: Instant = Instant.now(),

    var currentRevision: Int = 0,

    var compactedRevision: Int = 0
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Document) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}