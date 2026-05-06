package com.razer.engine.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record VoiceInputDTO(
        String sessionId,
        UUID messageId,
        String rawText,
        String transcript,
        Double confidence,
        String language,
        String intentJson
) {
}