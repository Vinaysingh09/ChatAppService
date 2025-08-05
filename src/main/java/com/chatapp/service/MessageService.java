package com.chatapp.service;

import com.chatapp.model.Chat;
import com.chatapp.model.Message;
import com.chatapp.model.Reaction;
import com.chatapp.model.User;
import com.chatapp.repository.ChatRepository;
import com.chatapp.repository.MessageRepository;
import com.chatapp.repository.ReactionRepository;
import com.chatapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class MessageService {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private ReactionRepository reactionRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Transactional
    public Message addReaction(Long messageId, Reaction.ReactionType type, String username) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));
        User user = userRepository.findByPhoneNumber(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Remove existing reaction if any
        reactionRepository.findByMessageIdAndUserId(messageId, user.getId())
                .ifPresent(reactionRepository::delete);

        // Add new reaction
        Reaction reaction = new Reaction();
        reaction.setMessage(message);
        reaction.setUser(user);
        reaction.setType(type);
        message.getReactions().add(reaction);
        
        Message updatedMessage = messageRepository.save(message);
        
        // Send WebSocket notification
        Map<String, Object> messageUpdate = new HashMap<>();
        messageUpdate.put("type", "MESSAGE_UPDATED");
        messageUpdate.put("data", updatedMessage);
        messagingTemplate.convertAndSend("/topic/chat/" + message.getChat().getId(), messageUpdate);
        
        return updatedMessage;
    }

    @Transactional
    public void removeReaction(Long messageId, String username) {
        User user = userRepository.findByPhoneNumber(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        reactionRepository.deleteByMessageIdAndUserId(messageId, user.getId());
        
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));
        
        // Send WebSocket notification
        Map<String, Object> messageUpdate = new HashMap<>();
        messageUpdate.put("type", "MESSAGE_UPDATED");
        messageUpdate.put("data", message);
        messagingTemplate.convertAndSend("/topic/chat/" + message.getChat().getId(), messageUpdate);
    }

    @Transactional
    public Message editMessage(Long messageId, String newContent, String username) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));
        
        if (!message.getSender().getUsername().equals(username)) {
            throw new RuntimeException("Not authorized to edit this message");
        }

        // Add current content to edit history
        message.getEditHistory().add(message.getContent());
        message.setContent(newContent);
        message.setEditedAt(LocalDateTime.now());
        
        Message updatedMessage = messageRepository.save(message);
        
        // Send WebSocket notification
        Map<String, Object> messageUpdate = new HashMap<>();
        messageUpdate.put("type", "MESSAGE_UPDATED");
        messageUpdate.put("data", updatedMessage);
        messagingTemplate.convertAndSend("/topic/chat/" + message.getChat().getId(), messageUpdate);
        
        return updatedMessage;
    }

    @Transactional
    public void deleteMessage(Long messageId, String username) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));
        
        if (!message.getSender().getPhoneNumber().equals(username)) {
            throw new RuntimeException("Not authorized to delete this message");
        }

        Long chatId = message.getChat().getId();
        messageRepository.delete(message);
        
        // Send WebSocket notification
        Map<String, Object> messageUpdate = new HashMap<>();
        messageUpdate.put("type", "MESSAGE_DELETED");
        messageUpdate.put("data", Map.of(
            "messageId", messageId,
            "chatId", chatId
        ));
        messagingTemplate.convertAndSend("/topic/chat/" + chatId, messageUpdate);
    }

    @Transactional
    public Message forwardMessage(Long messageId, Long targetChatId, String username) {
        Message originalMessage = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));
        Chat targetChat = chatRepository.findById(targetChatId)
                .orElseThrow(() -> new RuntimeException("Target chat not found"));
        User sender = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Message forwardedMessage = new Message();
        forwardedMessage.setChat(targetChat);
        forwardedMessage.setSender(sender);
        forwardedMessage.setMessageType(originalMessage.getMessageType());
        forwardedMessage.setContent(originalMessage.getContent());
        forwardedMessage.setMediaUrl(originalMessage.getMediaUrl());
        forwardedMessage.setForwarded(true);
        
        Message savedMessage = messageRepository.save(forwardedMessage);
        
        // Send WebSocket notification
        Map<String, Object> messageUpdate = new HashMap<>();
        messageUpdate.put("type", "MESSAGE_CREATED");
        messageUpdate.put("data", savedMessage);
        messagingTemplate.convertAndSend("/topic/chat/" + targetChatId, messageUpdate);
        
        return savedMessage;
    }

    public Page<Message> searchMessages(String query, Long chatId, Pageable pageable) {
        return messageRepository.searchMessages(chatId, query, pageable);
    }
} 