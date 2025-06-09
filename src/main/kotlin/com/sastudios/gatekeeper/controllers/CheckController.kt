package com.sastudios.gatekeeper.controllers

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class CheckController {
    @GetMapping("/check")
    fun check(): ResponseEntity<String> {
        return ResponseEntity.ok("API is running")
    }
}