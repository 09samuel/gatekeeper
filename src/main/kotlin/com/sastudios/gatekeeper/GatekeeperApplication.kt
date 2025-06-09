package com.sastudios.gatekeeper

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class GatekeeperApplication

fun main(args: Array<String>) {
	runApplication<GatekeeperApplication>(*args)
}
