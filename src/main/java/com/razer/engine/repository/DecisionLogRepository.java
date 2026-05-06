package com.razer.engine.repository;

import com.razer.engine.model.DecisionLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DecisionLogRepository extends JpaRepository<DecisionLog, UUID> {
    List<DecisionLog> findByMessage_IdOrderByCreatedAtAsc(UUID messageId);
}