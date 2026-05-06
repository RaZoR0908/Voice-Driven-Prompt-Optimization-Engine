package com.razer.engine.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record DecisionLogDTO(
        UUID messageId,
        String stepName,
        String decision,
        String detail,
        LocalDateTime createdAt
) {
}