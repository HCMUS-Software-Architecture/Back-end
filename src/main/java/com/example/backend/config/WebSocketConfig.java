package com.example.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${spring.rabbitmq.host:localhost}")
    private String rabbitMqHost;

    @Value("${spring.rabbitmq.stomp.port:61613}")
    private int stompPort;

    @Value("${spring.rabbitmq.username:guest}")
    private String rabbitMqUsername;

    @Value("${spring.rabbitmq.password:guest}")
    private String rabbitMqPassword;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableStompBrokerRelay("/topic")
                .setRelayHost(rabbitMqHost)
                .setRelayPort(stompPort)
                .setClientLogin(rabbitMqUsername)
                .setClientPasscode(rabbitMqPassword);
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/prices")
                .setAllowedOrigins("http://localhost:63342", "http://localhost:3000", "http://localhost:5500")
                .withSockJS();
    }
}
