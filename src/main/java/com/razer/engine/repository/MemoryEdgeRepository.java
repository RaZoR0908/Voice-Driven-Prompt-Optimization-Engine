package com.razer.engine.repository;

import com.razer.engine.model.MemoryEdge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MemoryEdgeRepository extends JpaRepository<MemoryEdge, UUID> {
}