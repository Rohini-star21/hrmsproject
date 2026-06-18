package com.mentis.hrms.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable a simple message broker for sending messages to clients
        config.enableSimpleBroker("/topic", "/queue", "/user");

        // Prefix for messages that are bound for @MessageMapping methods
        config.setApplicationDestinationPrefixes("/app");

        // Prefix for user-specific messages (for direct messaging to specific users)
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                // Allow all origins (adjust for production security)
                .setAllowedOriginPatterns("*")
                // Enable SockJS fallback for browsers that don't support WebSocket
                .withSockJS()
                // Configure SockJS options for better reliability
                .setStreamBytesLimit(512 * 1024)
                .setHttpMessageCacheSize(1000)
                .setDisconnectDelay(30 * 1000);

        // Optional: Add a plain WebSocket endpoint for native WebSocket support
        registry.addEndpoint("/ws-native")
                .setAllowedOriginPatterns("*");
    }
}