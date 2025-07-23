package com.chatapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class ChatApplication {
    public static void main(String[] args) {
        // Configure SpringApplication to prevent auto-shutdown
        SpringApplication app = new SpringApplication(ChatApplication.class);
        app.setRegisterShutdownHook(true);
        
        // Start the application and keep it running
        ConfigurableApplicationContext context = app.run(args);
        
        // Add shutdown hook to properly close resources
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("🛑 Application shutdown requested...");
            context.close();
        }));
        
        // Keep the application running
        System.out.println("🚀 ChatApplication started successfully!");
        System.out.println("📡 Server running on http://localhost:8081");
        System.out.println("🔌 WebSocket endpoint: ws://localhost:8081/ws");
        System.out.println("💡 Press Ctrl+C to stop the application");
    }
} 