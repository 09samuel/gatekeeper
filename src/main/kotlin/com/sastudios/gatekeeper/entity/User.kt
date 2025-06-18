package com.sastudios.gatekeeper.entity

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "users")
data class User(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, unique = true)
    val email: String,

    @Column(name = "name", nullable = false)
    val name: String,

    @Column(name = "hashed_password", nullable = false)
    val hashedPassword: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], orphanRemoval = true)
    val tokens: MutableList<RefreshToken> = mutableListOf(),
)