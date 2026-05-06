package com.razer.engine.repository;

import com.razer.engine.model.PromptLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PromptLogRepository extends JpaRepository<PromptLog, UUID> {
    List<PromptLog> findByMessage_IdOrderByCreatedAtAsc(UUID messageId);
}