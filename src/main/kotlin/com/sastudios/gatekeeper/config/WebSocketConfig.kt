package com.sastudios.gatekeeper.config

import com.sastudios.gatekeeper.websocket.DocumentWebSocketHandler
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry


@Configuration
@EnableWebSocket
class WebSocketConfig(
    private val documentWebSocketHandler: DocumentWebSocketHandler
) : WebSocketConfigurer {

   override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(documentWebSocketHandler, "/ws/doc/{docId}")
            .setAllowedOrigins("*")
    }
}
