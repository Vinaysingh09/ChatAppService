package com.chatapp.service;

import com.chatapp.dto.ChatRequest;
import com.chatapp.dto.MessageRequest;
import com.chatapp.dto.MessageResponse;
import com.chatapp.model.Chat;
import com.chatapp.model.Message;
import com.chatapp.model.User;
import com.chatapp.repository.ChatRepository;
import com.chatapp.repository.MessageRepository;
import com.chatapp.repository.UserRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatService(
            ChatRepository chatRepository,
            MessageRepository messageRepository,
            UserRepository userRepository,
            SimpMessagingTemplate messagingTemplate) {
        this.chatRepository = chatRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public List<Chat> getChatsForUser(String phoneNumber) {
        User user = getUserByPhoneNumber(phoneNumber);
        return chatRepository.findByParticipantsContaining(user);
    }

    @Transactional
    public Chat createChat(ChatRequest request, String phoneNumber) {
        User creator = getUserByPhoneNumber(phoneNumber);
        Set<User> participants = request.getParticipantIds().stream()
                .map(id -> userRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("User not found with id: " + id)))
                .collect(Collectors.toSet());
        
        participants.add(creator);

        Chat chat = new Chat();
        chat.setChatType(request.getChatType());
        chat.setName(request.getName());
        chat.setParticipants(participants);

        return chatRepository.save(chat);
    }

    public List<Message> getMessages(Long chatId, String phoneNumber) {
        User user = getUserByPhoneNumber(phoneNumber);
        Chat chat = getChatById(chatId);

        if (!chat.getParticipants().contains(user)) {
            throw new RuntimeException("User is not a participant of this chat");
        }

        return messageRepository.findByChatOrderBySentAtDesc(chat);
    }

    @Transactional
    public Message sendMessage(Long chatId, MessageRequest request, String phoneNumber) {
        try {
            System.out.println("📨 Processing message from " + phoneNumber + " to chat " + chatId);
            
            User sender = getUserByPhoneNumber(phoneNumber);
            Chat chat = getChatById(chatId);

            if (!chat.getParticipants().contains(sender)) {
                String error = "User is not a participant of this chat";
                System.err.println("❌ " + error);
                messagingTemplate.convertAndSendToUser(
                    phoneNumber,
                    "/queue/errors",
                    MessageResponse.error(error)
                );
                throw new RuntimeException(error);
            }

            Message message = new Message();
            message.setChat(chat);
            message.setSender(sender);
            message.setMessageType(request.getMessageType());
            message.setContent(request.getContent());
            message.setMediaUrl(request.getMediaUrl());

            System.out.println("💾 Saving message to database...");
            message = messageRepository.save(message);

            // Update chat's lastMessage
            chat.setLastMessage(message);
            chatRepository.save(chat);

            System.out.println("📡 Broadcasting message to chat participants...");
            
            // Notify all participants through WebSocket
            messagingTemplate.convertAndSend(
                "/topic/chat/" + chatId,
                message
            );

            // Send acknowledgment to sender with proper format
            System.out.println("✅ Sending acknowledgment to " + phoneNumber);
            messagingTemplate.convertAndSendToUser(
                phoneNumber,
                "/queue/message-sent",
                MessageResponse.success(message.getId())
            );

            return message;
        } catch (Exception e) {
            System.err.println("❌ Error sending message: " + e.getMessage());
            e.printStackTrace();
            
            // Send error back to sender with proper format
            messagingTemplate.convertAndSendToUser(
                phoneNumber,
                "/queue/errors",
                MessageResponse.error("Failed to send message: " + e.getMessage())
            );
            
            throw e;
        }
    }

    private User getUserByPhoneNumber(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new RuntimeException("User not found with phone number: " + phoneNumber));
    }

    private Chat getChatById(Long chatId) {
        return chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat not found with id: " + chatId));
    }
} 