package com.chatapp.dto;

import com.chatapp.model.Message;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MessageRequest {
    @NotNull(message = "Message type is required")
    @JsonProperty("type")  // Maps "type" from JSON to "messageType" field
    private Message.MessageType messageType;

    private String content;
    private String mediaUrl;

    // Manual getters to ensure compatibility
    public Message.MessageType getMessageType() {
        return messageType;
    }

    public String getContent() {
        return content;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }
} 