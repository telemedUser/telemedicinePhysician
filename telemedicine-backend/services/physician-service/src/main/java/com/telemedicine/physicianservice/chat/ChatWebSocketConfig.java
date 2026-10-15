package com.telemedicine.physicianservice.chat;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Real-time clinical chat over STOMP-on-WebSocket, using Spring's own open-source message broker
 * (spring-boot-starter-websocket, Apache 2.0) — no proprietary vendor SDK.
 *
 * <p>Clients connect to {@code /ws/chat} (SockJS fallback enabled), subscribe to {@code
 * /topic/consultation/{appointmentId}} to receive messages, and publish to {@code
 * /app/consultation/{appointmentId}/send}. Messages are persisted to Postgres and a PHI-free
 * {@code ChatMessageSent} marker is emitted so analytics/audit see the activity without the body
 * (consistent with the platform's existing chat event contract).
 *
 * <p>The simple in-memory broker is adequate for a single instance; moving to a Kafka/Redis-backed
 * relay later keeps the same client contract.
 */
@Configuration
@EnableWebSocketMessageBroker
public class ChatWebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Server → client destinations.
        registry.enableSimpleBroker("/topic");
        // Client → server destinations are prefixed with /app and routed to @MessageMapping methods.
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry
                .addEndpoint("/ws/chat")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
