package com.razer.engine.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PromptResponseDTO(
        String optimizedPrompt,
        int tokenInput,
        int tokenOutput,
        double reductionPct,
        String memoryDecision,
        UUID messageId,
        UUID memoryId
) {
}