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
public class IntentDetectionService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final double temperature;

    public IntentDetectionService(WebClient webClient,
                                  ObjectMapper objectMapper,
                                  @Value("${app.groq.api-key:}") String apiKey,
                                  @Value("${app.groq.base-url:https://api.groq.com/openai/v1}") String baseUrl,
                                  @Value("${app.groq.model:llama3-8b-8192}") String model,
                                  @Value("${app.groq.temperature:0.0}") double temperature) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
        this.temperature = temperature;
    }

    public IntentResponseDTO extractIntent(String text) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("GROQ_API_KEY is required");
        }

        Map<String, Object> request = Map.of(
                "model", model,
                "temperature", temperature,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt()),
                        Map.of("role", "user", "content", text)
                )
        );

        JsonNode response = webClient.post()
                .uri(baseUrl + "/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + apiKey)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block(Duration.ofSeconds(60));

        if (response == null) {
            throw new IllegalStateException("Groq returned no payload for intent extraction");
        }

        String content = response.path("choices").path(0).path("message").path("content").asText("");
        String json = extractJsonObject(content);
        try {
            return objectMapper.readValue(json, IntentResponseDTO.class);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to parse Groq intent JSON: " + exception.getMessage(), exception);
        }
    }

    private String systemPrompt() {
        return """
                You are an intent extractor. Return ONLY valid JSON.
                Schema:
                {
                  \"intent\": \"snake_case_intent_name\",
                  \"task\": \"one sentence describing what the user wants\",
                  \"domain\": \"single word domain\",
                  \"constraints\": [\"constraint1\", \"constraint2\"],
                  \"output_format\": \"bullet_list|paragraph|table|code|numbered_list|short_answer\",
                  \"audience\": \"general|developer|business|student|expert\"
                }
                No markdown, no commentary, no code fences.
                """;
    }

    private String extractJsonObject(String content) {
        String trimmed = content == null ? "" : content.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "");
        }
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }
}