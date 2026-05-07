package com.razer.engine.repository;

import com.razer.engine.model.Memory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MemoryRepository extends JpaRepository<Memory, UUID> {
    List<Memory> findAllByOrderByUpdatedAtDesc();

    List<Memory> findBySessionIdOrderByUpdatedAtDesc(String sessionId);
}