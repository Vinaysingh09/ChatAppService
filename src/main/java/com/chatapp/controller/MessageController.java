package com.chatapp.controller;

import com.chatapp.dto.*;
import com.chatapp.model.Message;
import com.chatapp.model.Reaction;
import com.chatapp.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @PostMapping("/{messageId}/reactions")
    public ResponseEntity<Map<String, Boolean>> addReaction(
            @PathVariable Long messageId,
            @RequestBody ReactionRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        messageService.addReaction(messageId, request.getType(), userDetails.getUsername());
        return ResponseEntity.ok(Map.of("success", true));
    }

    @DeleteMapping("/{messageId}/reactions")
    public ResponseEntity<Map<String, Boolean>> removeReaction(
            @PathVariable Long messageId,
            @AuthenticationPrincipal UserDetails userDetails) {
        messageService.removeReaction(messageId, userDetails.getUsername());
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PutMapping("/{messageId}")
    public ResponseEntity<Message> editMessage(
            @PathVariable Long messageId,
            @RequestBody MessageEditRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(messageService.editMessage(messageId, request.getContent(), userDetails.getUsername()));
    }

    @DeleteMapping("/{messageId}")
    public ResponseEntity<Map<String, Boolean>> deleteMessage(
            @PathVariable Long messageId,
            @AuthenticationPrincipal UserDetails userDetails) {
        messageService.deleteMessage(messageId, userDetails.getUsername());
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/{messageId}/forward")
    public ResponseEntity<Message> forwardMessage(
            @PathVariable Long messageId,
            @RequestBody MessageForwardRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(messageService.forwardMessage(messageId, request.getTargetChatId(), userDetails.getUsername()));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<Message>> searchMessages(
            @RequestParam String query,
            @RequestParam Long chatId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(messageService.searchMessages(query, chatId, PageRequest.of(page, size)));
    }
} 