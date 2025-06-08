package com.sastudios.gatekeeper.repository

import com.sastudios.gatekeeper.entity.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long> {
    fun findByEmail(email: String): User?
}