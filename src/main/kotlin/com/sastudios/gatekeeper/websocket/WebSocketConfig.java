package com.sastudios.gatekeeper.websocket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    @Autowired
    DocumentWebSocketHandler handler;
    public void registerWebSocketHandlers(WebSocketHandlerRegistry r) {
        r.addHandler(handler, "/ws/documents").setAllowedOrigins("*");
    }
}