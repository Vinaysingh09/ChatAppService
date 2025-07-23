package com.chatapp.repository;

import com.chatapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByPhoneNumber(String phoneNumber);
    boolean existsByPhoneNumber(String phoneNumber);
    
    // MySQL 8.0+ compatible search query with REGEXP_REPLACE
    @Query(value = "SELECT * FROM users u WHERE " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "u.phone_number LIKE CONCAT('%', :query, '%') OR " +
           "REGEXP_REPLACE(u.phone_number, '[^0-9]', '') LIKE CONCAT('%', REGEXP_REPLACE(:query, '[^0-9]', ''), '%')", 
           nativeQuery = true)
    List<User> searchUsers(@Param("query") String query);
    
    // Find users by username containing the query (case-insensitive)
    List<User> findByUsernameContainingIgnoreCase(String username);
    
    // Simple phone search method for fallback (compatible with all MySQL versions)
    @Query(value = "SELECT * FROM users u WHERE u.phone_number LIKE CONCAT('%', :phoneNumber, '%')", 
           nativeQuery = true)
    List<User> findByPhoneNumberContaining(@Param("phoneNumber") String phoneNumber);
    
    // Alternative search without regex for older MySQL versions (5.7 and below)
    @Query(value = "SELECT * FROM users u WHERE " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "u.phone_number LIKE CONCAT('%', :query, '%')", 
           nativeQuery = true)
    List<User> searchUsersSimple(@Param("query") String query);
    
    // Additional search patterns for phone numbers (supports multiple variations)
    @Query(value = "SELECT * FROM users u WHERE " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "u.phone_number LIKE CONCAT('%', :query, '%') OR " +
           "REPLACE(REPLACE(REPLACE(u.phone_number, '+', ''), '-', ''), ' ', '') LIKE CONCAT('%', REPLACE(REPLACE(REPLACE(:query, '+', ''), '-', ''), ' ', ''), '%')", 
           nativeQuery = true)
    List<User> searchUsersWithReplace(@Param("query") String query);
} 