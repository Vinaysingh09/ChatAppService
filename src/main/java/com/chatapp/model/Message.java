package com.chatapp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "messages")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "chat_id", nullable = false)
    @JsonIgnoreProperties({"participants", "lastMessage", "hibernateLazyInitializer", "handler"}) // Prevent circular reference and handle proxies
    private Chat chat;

    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User sender;

    @Enumerated(EnumType.STRING)
    private MessageType messageType;

    @Column(columnDefinition = "TEXT")
    private String content;

    private String mediaUrl;

    @Column(nullable = false)
    private LocalDateTime sentAt;

    @PrePersist
    protected void onCreate() {
        sentAt = LocalDateTime.now();
    }

    public enum MessageType {
        TEXT,
        IMAGE,
        VIDEO,
        AUDIO,
        FILE
    }

    // Manual getters and setters to ensure compatibility
    public Chat getChat() {
        return chat;
    }

    public void setChat(Chat chat) {
        this.chat = chat;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
    
} 