package com.chatapp.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.Instant;

@Data
public class MessageResponse {
    private String messageId;  // Changed to String
    private String status;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant timestamp;

    public MessageResponse() {
        this.timestamp = Instant.now();
    }

    public MessageResponse(String messageId, String status) {  // Updated constructor
        this.messageId = messageId;
        this.status = status;
        this.timestamp = Instant.now();
    }

    public static MessageResponse success(Long messageId) {
        return new MessageResponse(messageId.toString(), "sent");  // Convert Long to String
    }

    public static MessageResponse error(String errorMessage) {
        MessageResponse response = new MessageResponse();
        response.setStatus("error");
        return response;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {  // Updated setter
        this.messageId = messageId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
} 