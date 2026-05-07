package com.razer.engine.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.razer.engine.dto.IntentResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class PromptTransformService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String baseUrl;
    private final String model;

    public PromptTransformService(WebClient webClient,
                                  ObjectMapper objectMapper,
                                  @Value("${groq.api-key}") String apiKey,
                                  @Value("${groq.base-url}") String baseUrl,
                                  @Value("${groq.model}") String model) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
    }

    public String optimize(IntentResponseDTO intent) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("GROQ_API_KEY is required");
        }

        Map<String, Object> request = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt()),
                        Map.of("role", "user", "content", buildUserMessage(intent))
                ),
                "temperature", 0,
                "max_tokens", 100
        );

        String responseBody = webClient.post()
                .uri(baseUrl + "/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + apiKey)
                .bodyValue(request)
                .retrieve()
                .onStatus(
                        status -> status.isError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .map(body -> new RuntimeException("Groq Error: " + body))
                )
                .bodyToMono(String.class)
                .block(Duration.ofSeconds(60));

        JsonNode response;
        try {
            response = objectMapper.readTree(responseBody);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to parse Groq response", exception);
        }

        String content = response.path("choices")
                .path(0)
                .path("message")
                .path("content")
                .asText("");

        return stripCodeFences(content).trim();
    }

    private String systemPrompt() {
        return """
                You are a prompt optimizer working in CAVEMAN MODE.
                
                GOAL: Take the user's intent and compress it into the shortest possible prompt.
                
                STRICT RULES:
                - Output must be FEWER words than the input task
                - Maximum 12 words total
                - Do NOT add any new information not present in the input
                - Do NOT add step counts like "3-step" or "5-step" unless user said so
                - Do NOT add word limits unless user specified one
                - Do NOT add format specs unless user specified one
                - Remove ALL filler words, politeness, redundancy
                - Keep ONLY: action verb + subject + explicit constraints
                - Output ONLY the compressed prompt — no explanation, no quotes
                
                Examples:
                
                Task: "Create a social media strategy for a fitness app with step by step instructions and keep it short"
                Output: Create short step-by-step social media strategy for fitness app.
                
                Task: "Write a marketing plan for my gym app in bullet points under 100 words"
                Output: Write gym app marketing plan. Format: bullets, under 100 words.
                
                Task: "Help me build a REST API for user authentication in Java"
                Output: Build Java REST API for user authentication.
                
                Task: "I want to write a blog post about AI trends for developers"
                Output: Write developer-focused blog post on AI trends.
                """;
    }

    private String buildUserMessage(IntentResponseDTO intent) {
        StringBuilder sb = new StringBuilder();
        sb.append("Task: ").append(intent.task());
        if (intent.constraints() != null && !intent.constraints().isEmpty()) {
            sb.append("\nConstraints: ").append(String.join(", ", intent.constraints()));
        }
        if (intent.outputFormat() != null && !intent.outputFormat().isBlank()) {
            sb.append("\nFormat: ").append(intent.outputFormat());
        }
        return sb.toString();
    }

    private String toJson(IntentResponseDTO intent) {
        try {
            return objectMapper.writeValueAsString(intent);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to serialize intent JSON", exception);
        }
    }

    private String stripCodeFences(String content) {
        String trimmed = content == null ? "" : content.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed
                    .replaceFirst("^```(?:text|json)?\\s*", "")
                    .replaceFirst("\\s*```$", "");
        }
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\""))
                || (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }
}