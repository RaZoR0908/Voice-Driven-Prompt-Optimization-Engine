package com.razer.engine.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MemoryCardDTO(
        UUID id,
        String domain,
        String task,
        String optimizedPrompt,
        int useCount,
        LocalDateTime updatedAt
) {
}