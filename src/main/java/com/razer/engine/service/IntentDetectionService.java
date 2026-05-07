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

    public IntentDetectionService(WebClient webClient,
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

    public IntentResponseDTO extractIntent(String text) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("GROQ_API_KEY is required");
        }

        Map<String, Object> request = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt()),
                        Map.of("role", "user", "content", text)
                ),
                "temperature", 0,
                "max_tokens", 300
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

        String json = extractJsonObject(content);

        try {
            return objectMapper.readValue(json, IntentResponseDTO.class);
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to parse Groq intent JSON: " + exception.getMessage(),
                    exception
            );
        }
    }

    private String systemPrompt() {
        return """
                You are an intent extractor. Extract intent from user input.
                Return ONLY valid JSON. No markdown. No explanation. No extra text.
                
                STRICT RULES for constraints:
                - Only include constraints that are EXPLICITLY stated by the user
                - Do NOT infer or assume constraints not mentioned
                - Do NOT add target audience, word limits, or format as constraints unless user said so
                - If no constraints are mentioned, return an empty array []
                
                Schema:
                {
                  "intent": "snake_case_intent_name",
                  "task": "one sentence describing what the user wants",
                  "domain": "single word domain",
                  "constraints": ["only explicit constraints from user input"],
                  "output_format": "bullet_list|paragraph|table|code|numbered_list|short_answer",
                  "audience": "general|developer|business|student|expert"
                }
                
                Examples:
                
                Input: "Create a marketing plan for gym app"
                Output: {
                  "intent": "create_marketing_plan",
                  "task": "Create a marketing plan for gym app",
                  "domain": "marketing",
                  "constraints": [],
                  "output_format": "paragraph",
                  "audience": "business"
                }
                
                Input: "Write a blog post about AI trends for developers in bullet points under 500 words"
                Output: {
                  "intent": "write_blog_post_ai_trends",
                  "task": "Write a blog post about AI trends for developers",
                  "domain": "ai",
                  "constraints": ["bullet points", "under 500 words"],
                  "output_format": "bullet_list",
                  "audience": "developer"
                }
                
                No markdown. No code fences. JSON only.
                """;
    }

    private String extractJsonObject(String content) {
        String trimmed = content == null ? "" : content.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed
                    .replaceFirst("^```(?:json)?\\s*", "")
                    .replaceFirst("\\s*```$", "");
        }
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }
}