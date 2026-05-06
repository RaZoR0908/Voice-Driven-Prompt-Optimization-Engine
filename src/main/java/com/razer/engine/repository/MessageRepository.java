package com.razer.engine.repository;

import com.razer.engine.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {
    List<Message> findByConversation_SessionIdOrderByCreatedAtAsc(String sessionId);

    Optional<Message> findFirstByConversation_SessionIdOrderByCreatedAtDesc(String sessionId);
}