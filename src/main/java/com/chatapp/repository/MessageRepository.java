package com.chatapp.repository;

import com.chatapp.model.Chat;
import com.chatapp.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByChatOrderBySentAtDesc(Chat chat);
    
    @Query("SELECT m FROM Message m WHERE m.chat.id = :chatId AND LOWER(m.content) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY m.sentAt DESC")
    Page<Message> searchMessages(@Param("chatId") Long chatId, @Param("query") String query, Pageable pageable);
    
    @Query("SELECT m FROM Message m WHERE m.chat.id = :chatId AND m.sentAt > :lastMessageTime ORDER BY m.sentAt ASC")
    List<Message> findNewMessages(@Param("chatId") Long chatId, @Param("lastMessageTime") java.time.LocalDateTime lastMessageTime);
} 