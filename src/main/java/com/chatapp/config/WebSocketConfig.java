package com.chatapp.config;

import com.chatapp.security.WebSocketAuthenticationInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.messaging.simp.config.ChannelRegistration;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${websocket.allowed-origins}")
    private String allowedOrigins;

    @Value("${websocket.endpoint}")
    private String endpoint;

    @Value("${websocket.topic-prefix}")
    private String topicPrefix;

    @Value("${websocket.application-prefix}")
    private String applicationPrefix;

    private final WebSocketAuthenticationInterceptor authenticationInterceptor;

    public WebSocketConfig(WebSocketAuthenticationInterceptor authenticationInterceptor) {
        this.authenticationInterceptor = authenticationInterceptor;
        System.out.println("🔧 WebSocket Configuration Initialized");
        System.out.println("📍 Endpoint: " + endpoint);
        System.out.println("🔒 Allowed Origins: " + allowedOrigins);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        System.out.println("\n🔌 Registering WebSocket endpoints...");
        System.out.println("📍 Adding endpoint: " + endpoint);
        System.out.println("🔒 Setting allowed origins: " + allowedOrigins);
        
        registry.addEndpoint(endpoint)
                .addInterceptors(authenticationInterceptor)
                .setAllowedOriginPatterns("*") // More permissive for testing
                .withSockJS()
                .setWebSocketEnabled(true)
                .setDisconnectDelay(30 * 1000)
                .setHeartbeatTime(25 * 1000);
                
        System.out.println("✅ WebSocket endpoints registered successfully");
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration
            .setSendTimeLimit(20 * 1000) // 20 seconds
            .setSendBufferSizeLimit(512 * 1024) // 512KB
            .setMessageSizeLimit(128 * 1024) // 128KB
            .setTimeToFirstMessage(30 * 1000); // 30 seconds
            
        System.out.println("⚙️ WebSocket transport configured:");
        System.out.println("📤 Send timeout: 20 seconds");
        System.out.println("📦 Buffer size: 512KB");
        System.out.println("📝 Message size: 128KB");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.taskExecutor()
            .corePoolSize(4)
            .maxPoolSize(10)
            .queueCapacity(50);
            
        System.out.println("⚙️ Inbound channel configured:");
        System.out.println("🧵 Core threads: 4");
        System.out.println("🧵 Max threads: 10");
        System.out.println("📑 Queue capacity: 50");
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        registration.taskExecutor()
            .corePoolSize(4)
            .maxPoolSize(10)
            .queueCapacity(50);
            
        System.out.println("⚙️ Outbound channel configured:");
        System.out.println("🧵 Core threads: 4");
        System.out.println("🧵 Max threads: 10");
        System.out.println("📑 Queue capacity: 50");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        System.out.println("\n📨 Configuring message broker...");
        System.out.println("📍 Application destination prefix: " + applicationPrefix);
        System.out.println("📍 Topic prefix: " + topicPrefix);
        
        registry.setApplicationDestinationPrefixes(applicationPrefix);
        registry.enableSimpleBroker(topicPrefix, "/queue")
               .setHeartbeatValue(new long[]{10000, 10000})
               .setTaskScheduler(new ConcurrentTaskScheduler());
        
        System.out.println("✅ Message broker configured successfully");
        System.out.println("💓 Heartbeat interval: 10 seconds");
    }
} 