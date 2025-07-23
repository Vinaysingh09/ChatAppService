package com.chatapp.dto;

import com.chatapp.model.Chat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ChatRequest {
    @NotNull(message = "Chat type is required")
    @JsonProperty("type")  // Maps "type" from JSON to "chatType" field
    private Chat.ChatType chatType;

    private String name;

    @NotEmpty(message = "Participant IDs are required")
    private List<Long> participantIds;

    // Manual getters to ensure compatibility
    public Chat.ChatType getChatType() {
        return chatType;
    }

    public String getName() {
        return name;
    }

    public List<Long> getParticipantIds() {
        return participantIds;
    }
} 