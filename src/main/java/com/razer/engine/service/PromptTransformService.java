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
    private final double temperature;

    public PromptTransformService(WebClient webClient,
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

    public String optimize(IntentResponseDTO intent) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("GROQ_API_KEY is required");
        }

        Map<String, Object> request = Map.of(
                "model", model,
                "temperature", temperature,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt()),
                        Map.of("role", "user", "content", toJson(intent))
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
            throw new IllegalStateException("Groq returned no payload for prompt transformation");
        }

        String content = response.path("choices").path(0).path("message").path("content").asText("");
        return stripCodeFences(content).trim();
    }

    private String systemPrompt() {
        return """
                You are a prompt optimizer working in CAVEMAN MODE.
                Generate ONE minimal prompt.
                Rules:
                - Remove all filler words
                - No greetings, no explanation, no preamble
                - Max 2 sentences
                - Include action verb, subject, output format, and constraints
                - Output only the final prompt
                """;
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
            trimmed = trimmed.replaceFirst("^```(?:text)?\\s*", "").replaceFirst("\\s*```$", "");
        }
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\"")) || (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }
}