package com.razer.engine.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class LanguageNormalizer {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String baseUrl;
    private final String model;

    public LanguageNormalizer(WebClient webClient,
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

    public NormalizedText normalize(String input) {
        String text = input == null ? "" : input.trim().replaceAll("\\s+", " ");
        String language = detectLanguage(text);

        String normalized = text;
        if (!language.equals("en")) {
            try {
                normalized = translateViaGroq(text);
            } catch (Exception e) {
                normalized = text; // fallback to original if Groq fails
            }
        }

        return new NormalizedText(normalized, language);
    }

    public String detectLanguage(String input) {
        if (input == null || input.isBlank()) return "en";

        if (containsDevanagari(input)) {
            // Check if it also has English words mixed in
            String lower = input.toLowerCase(Locale.ROOT);
            boolean hasEnglish = lower.matches(".*[a-z]+.*");
            return hasEnglish ? "mr-mix" : "hi";
        }

        // Use Groq for language detection
        try {
            return detectLanguageViaGroq(input);
        } catch (Exception e) {
            return "en"; // fallback to English if Groq fails
        }
    }

    private String detectLanguageViaGroq(String text) {
        Map<String, Object> request = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", """
                                Detect the language of the following text.
                                Output ONLY one of these labels: en, hi, hinglish, mr, mr-mix
                                Do not output anything else, no explanation, no notes.
                                
                                Examples:
                                - "Help me write a blog post about AI" → en
                                - "Ek marketing plan bana do for gym app" → hinglish
                                - "mujhe ek REST API chahiye Java mein" → hinglish
                                - "Write a Python script to scrape websites" → en
                                - "Mazya sathi ek plan banav" → mr-mix
                                """),
                        Map.of("role", "user", "content", text)
                ),
                "temperature", 0,
                "max_tokens", 5
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
                .block(Duration.ofSeconds(30));

        JsonNode response;
        try {
            response = objectMapper.readTree(responseBody);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to parse Groq response", e);
        }

        String label = response.path("choices")
                .path(0)
                .path("message")
                .path("content")
                .asText("")
                .trim()
                .toLowerCase();

        // Validate the label is one of the expected values
        if (label.matches("^(en|hi|hinglish|mr|mr-mix)$")) {
            return label;
        }

        return "en"; // fallback if unexpected label
    }

    private String translateViaGroq(String text) {
        Map<String, Object> request = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", """
                                Translate the following text to English.
                                Rules:
                                - Preserve the original meaning exactly
                                - Output ONLY the English translation
                                - No explanation, no notes, no quotes around the output
                                - Handle mixed Hindi-English (Hinglish) and Marathi naturally
                                """),
                        Map.of("role", "user", "content", text)
                ),
                "temperature", 0,
                "max_tokens", 200
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
                .block(Duration.ofSeconds(30));

        JsonNode response;
        try {
            response = objectMapper.readTree(responseBody);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to parse Groq response", e);
        }

        return response.path("choices")
                .path(0)
                .path("message")
                .path("content")
                .asText("")
                .trim();
    }

    private boolean containsDevanagari(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= '\u0900' && c <= '\u097F') return true;
        }
        return false;
    }

    public record NormalizedText(String text, String language) {}
}