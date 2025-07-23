package com.chatapp.dto;

public class AuthResponse {
    private String token;

    // Explicit constructor
    public AuthResponse(String token) {
        this.token = token;
    }

    // Default constructor for JSON deserialization
    public AuthResponse() {
    }

    // Manual getter and setter
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
} 