package com.sastudios.gatekeeper

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class GatekeeperApplication

fun main(args: Array<String>) {
	runApplication<GatekeeperApplication>(*args)
}
