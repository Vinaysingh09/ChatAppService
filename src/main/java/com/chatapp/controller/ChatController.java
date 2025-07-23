package com.chatapp.controller;

import com.chatapp.dto.ChatRequest;
import com.chatapp.dto.MessageRequest;
import com.chatapp.model.Chat;
import com.chatapp.model.Message;
import com.chatapp.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chats")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping
    public ResponseEntity<List<Chat>> getChats(@AuthenticationPrincipal UserDetails userDetails) {
        List<Chat> chats = chatService.getChatsForUser(userDetails.getUsername());
        return ResponseEntity.ok(chats);
    }

    @PostMapping
    public ResponseEntity<Chat> createChat(
            @Valid @RequestBody ChatRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Chat chat = chatService.createChat(request, userDetails.getUsername());
        return ResponseEntity.ok(chat);
    }

    @GetMapping("/{chatId}/messages")
    public ResponseEntity<List<Message>> getMessages(
            @PathVariable Long chatId,
            @AuthenticationPrincipal UserDetails userDetails) {
        List<Message> messages = chatService.getMessages(chatId, userDetails.getUsername());
        return ResponseEntity.ok(messages);
    }

    @PostMapping("/{chatId}/messages")
    public ResponseEntity<Message> sendMessage(
            @PathVariable Long chatId,
            @Valid @RequestBody MessageRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Message message = chatService.sendMessage(chatId, request, userDetails.getUsername());
        return ResponseEntity.ok(message);
    }
} 