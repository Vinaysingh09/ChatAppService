package com.chatapp.dto;

import lombok.Data;

@Data
public class UserUpdateRequest {
    private String username;
    private String profilePictureUrl;
    private String status;

    // Manual getters to ensure compatibility
    public String getUsername() {
        return username;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public String getStatus() {
        return status;
    }
} 