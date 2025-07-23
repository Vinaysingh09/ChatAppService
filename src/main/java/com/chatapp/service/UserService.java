package com.chatapp.service;

import com.chatapp.dto.UserUpdateRequest;
import com.chatapp.model.User;
import com.chatapp.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getUserByPhoneNumber(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new RuntimeException("User not found with phone number: " + phoneNumber));
    }

    @Transactional
    public User updateUser(String phoneNumber, UserUpdateRequest request) {
        User user = getUserByPhoneNumber(phoneNumber);

        if (request.getUsername() != null) {
            user.setUsername(request.getUsername());
        }
        if (request.getProfilePictureUrl() != null) {
            user.setProfilePictureUrl(request.getProfilePictureUrl());
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }

        user.setLastSeen(LocalDateTime.now());
        return userRepository.save(user);
    }

    public List<User> searchUsers(String query, String currentUserPhoneNumber) {
        if (query == null || query.trim().isEmpty()) {
            return List.of(); // Return empty list for empty queries
        }
        
        String trimmedQuery = query.trim();
        logger.info("Searching users with query: '{}', current user: '{}'", trimmedQuery, currentUserPhoneNumber);
        
        List<User> users = List.of();
        
        // Try different search methods in order of compatibility
        try {
            // Method 1: Try REGEXP_REPLACE (MySQL 8.0+)
            users = userRepository.searchUsers(trimmedQuery);
            logger.info("REGEXP_REPLACE search found {} users", users.size());
        } catch (Exception e1) {
            logger.warn("REGEXP_REPLACE search failed, trying REPLACE method: {}", e1.getMessage());
            try {
                // Method 2: Try nested REPLACE functions (MySQL 5.7+)
                users = userRepository.searchUsersWithReplace(trimmedQuery);
                logger.info("REPLACE search found {} users", users.size());
            } catch (Exception e2) {
                logger.warn("REPLACE search failed, using simple search: {}", e2.getMessage());
                // Method 3: Fallback to simple search (all MySQL versions)
                users = userRepository.searchUsersSimple(trimmedQuery);
                logger.info("Simple search found {} users", users.size());
            }
        }
        
        // If still no results and query looks like a phone number, try direct phone search
        if (users.isEmpty() && isPhoneNumberQuery(trimmedQuery)) {
            logger.info("No results found, trying direct phone number search");
            users = userRepository.findByPhoneNumberContaining(trimmedQuery);
            logger.info("Direct phone search found {} users", users.size());
        }
        
        // Log all found users for debugging
        users.forEach(user -> 
            logger.info("Found user: id={}, username='{}', phoneNumber='{}'", 
                user.getId(), user.getUsername(), user.getPhoneNumber())
        );
        
        // Exclude the current user from search results
        List<User> filteredUsers = users.stream()
                .filter(user -> !user.getPhoneNumber().equals(currentUserPhoneNumber))
                .collect(Collectors.toList());
                
        logger.info("Returning {} users after filtering out current user", filteredUsers.size());
        return filteredUsers;
    }
    
    // Helper method to check if query looks like a phone number
    private boolean isPhoneNumberQuery(String query) {
        // Remove common phone number characters and check if mostly digits
        String cleanQuery = query.replaceAll("[^0-9]", "");
        return cleanQuery.length() >= 3 && cleanQuery.length() >= query.length() * 0.5;
    }
    
    // Debug method to get all users - remove in production
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
} 