package com.chatapp.controller;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.chatapp.dto.MessageRequest;
import com.chatapp.dto.MessageResponse;
import com.chatapp.model.Message;
import com.chatapp.service.ChatService;

@Controller
public class WebSocketMessageController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketMessageController(ChatService chatService, SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chat/{chatId}/send")
    public void sendMessage(
            @DestinationVariable Long chatId,
            @Payload MessageRequest messageRequest,
            SimpMessageHeaderAccessor headerAccessor) {
        
        String phoneNumber = null;
        try {
            System.out.println("📨 Received message request for chat " + chatId);
            
            // Get username from WebSocket session attributes (set during handshake)
            phoneNumber = (String) headerAccessor.getSessionAttributes().get("username");
            System.out.println("👤 User phone number: " + phoneNumber);
            
            if (phoneNumber == null) {
                System.err.println("❌ WebSocket message rejected: No authenticated user");
                return; // Can't send error without phone number
            }
            
            // Validate message request
            if (messageRequest == null) {
                throw new IllegalArgumentException("Message request cannot be null");
            }
            
            // Send message through chat service
            System.out.println("📤 Sending message to chat service...");
            Message message = chatService.sendMessage(chatId, messageRequest, phoneNumber);
            
            // Send acknowledgment back to sender with proper format
            System.out.println("✅ Message saved with ID: " + message.getId());
//            MessageResponse savedMessage = MessageResponse.success(message.getId());
            Map<String, Object> response = new HashMap<>();
            response.put("messageId", message.getId().toString());
            response.put("status", "sent");
            response.put("timestamp", Instant.now().toString());
            System.out.println("📤 Response: " + response);
            messagingTemplate.convertAndSendToUser(
                phoneNumber,
                "/queue/message-sent",
                response
            );
            
        } catch (Exception e) {
            System.err.println("❌ Error processing message: " + e.getMessage());
            e.printStackTrace();
            
            if (phoneNumber != null) {
                try {
                    MessageResponse errorResponse = MessageResponse.error(e.getMessage());
                    System.out.println("📤 Error response: " + errorResponse);
                    
                    messagingTemplate.convertAndSendToUser(
                        phoneNumber,
                        "/queue/errors",
                        errorResponse
                    );
                } catch (Exception sendError) {
                    System.err.println("❌ Failed to send error message: " + sendError.getMessage());
                    sendError.printStackTrace();
                }
            }
        }
    }
    
    @MessageMapping("/chat/{chatId}/typing")
    public void handleTyping(
            @DestinationVariable Long chatId,
            SimpMessageHeaderAccessor headerAccessor) {
        
        try {
            String phoneNumber = (String) headerAccessor.getSessionAttributes().get("username");
            
            if (phoneNumber != null) {
                // Broadcast typing indicator to other participants in the chat
                messagingTemplate.convertAndSend(
                    "/topic/chat/" + chatId + "/typing",
                    phoneNumber + " is typing..."
                );
            }
        } catch (Exception e) {
            System.err.println("Error handling typing indicator: " + e.getMessage());
        }
    }
    
    @MessageMapping("/heartbeat")
    public void handleHeartbeat(SimpMessageHeaderAccessor headerAccessor) {
        String phoneNumber = (String) headerAccessor.getSessionAttributes().get("username");
        
        if (phoneNumber != null) {
            MessageResponse response = new MessageResponse();
            response.setStatus("pong");
            
            messagingTemplate.convertAndSendToUser(
                phoneNumber,
                "/queue/heartbeat",
                response
            );
        }
    }
    
    @MessageMapping("/user/status")
    public void updateUserStatus(
            @Payload String status,
            SimpMessageHeaderAccessor headerAccessor) {
        
        try {
            String phoneNumber = (String) headerAccessor.getSessionAttributes().get("username");
            
            if (phoneNumber != null) {
                MessageResponse response = new MessageResponse();
                response.setStatus(status);
                
                messagingTemplate.convertAndSend(
                    "/topic/user/" + phoneNumber + "/status",
                    response
                );
            }
        } catch (Exception e) {
            System.err.println("Error updating user status: " + e.getMessage());
        }
    }
} 