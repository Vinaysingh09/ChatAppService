package com.chatapp.controller;

import com.chatapp.dto.UserUpdateRequest;
import com.chatapp.model.User;
import com.chatapp.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<User> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserByPhoneNumber(userDetails.getUsername());
        return ResponseEntity.ok(user);
    }

    @PutMapping("/me")
    public ResponseEntity<User> updateCurrentUser(
            @Valid @RequestBody UserUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User updatedUser = userService.updateUser(userDetails.getUsername(), request);
        return ResponseEntity.ok(updatedUser);
    }

    @GetMapping("/search")
    public ResponseEntity<List<User>> searchUsers(
            @RequestParam(value = "query", required = false, defaultValue = "") String query,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        // Return empty list if query is null or empty after trimming
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.ok(List.of());
        }
        
        List<User> users = userService.searchUsers(query, userDetails.getUsername());
        return ResponseEntity.ok(users);
    }
    
    // Debug endpoint to help troubleshoot search issues
    @GetMapping("/debug/all")
    public ResponseEntity<List<User>> getAllUsers() {
        // This is for debugging only - remove in production
        return ResponseEntity.ok(userService.getAllUsers());
    }
    
    @GetMapping("/debug/search")
    public ResponseEntity<List<User>> debugSearch(
            @RequestParam String query,
            @AuthenticationPrincipal UserDetails userDetails) {
        // This shows detailed search results for debugging
        return ResponseEntity.ok(userService.searchUsers(query, userDetails.getUsername()));
    }
} 