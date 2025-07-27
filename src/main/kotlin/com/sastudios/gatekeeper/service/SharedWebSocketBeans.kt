package com.sastudios.gatekeeper.service

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.WebSocketSession
import java.util.concurrent.ConcurrentHashMap

@Configuration
class SharedWebSocketBeans {

    @Bean
    fun webSocketSessionsMap(): ConcurrentHashMap<String, MutableSet<WebSocketSession>> {
        return ConcurrentHashMap()
    }
}
