package com.razer.engine.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ChatHistoryDTO(
        UUID messageId,
        String rawText,
        String transcript,
        String optimizedPrompt,
        Integer tokenInput,
        Integer tokenOutput,
        Double reductionPct,
        LocalDateTime createdAt
) {
}