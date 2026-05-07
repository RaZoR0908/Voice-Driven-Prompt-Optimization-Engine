package com.razer.engine.service;

import com.razer.engine.dto.IntentResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ValidationService {

    private final TokenOptimizerService tokenOptimizerService;
    private final double minimumReductionPct;

    // If input is under this many tokens, skip reduction threshold check
    private static final int SHORT_INPUT_THRESHOLD = 20;

    public ValidationService(TokenOptimizerService tokenOptimizerService,
                             @Value("${app.validation.min-reduction-pct:30}") double minimumReductionPct) {
        this.tokenOptimizerService = tokenOptimizerService;
        this.minimumReductionPct = minimumReductionPct;
    }

    public ValidationResult validate(IntentResponseDTO intent, String sourceText, String optimizedPrompt) {
        if (optimizedPrompt == null || optimizedPrompt.isBlank()) {
            return new ValidationResult(false, "Optimized prompt is empty", 0.0);
        }

        TokenOptimizerService.TokenStats stats = tokenOptimizerService.calculate(sourceText, optimizedPrompt);

        // Skip reduction check for short inputs — already minimal
        boolean isShortInput = stats.inputTokens() <= SHORT_INPUT_THRESHOLD;

        if (!isShortInput && stats.reductionPct() < minimumReductionPct) {
            return new ValidationResult(false,
                    "Token reduction is below the configured threshold",
                    stats.reductionPct());
        }

        if (!isAligned(intent, optimizedPrompt)) {
            return new ValidationResult(false,
                    "Optimized prompt is not aligned with the extracted intent",
                    stats.reductionPct());
        }

        return new ValidationResult(true,
                "Token reduction: " + String.format("%.0f", stats.reductionPct()) + "%",
                stats.reductionPct());
    }

    private boolean isAligned(IntentResponseDTO intent, String optimizedPrompt) {
        Set<String> intentTokens = keywordTokens(
                intent.task() + " " + intent.domain() + " " + String.join(" ", intent.constraints())
        );
        Set<String> promptTokens = keywordTokens(optimizedPrompt);
        intentTokens.retainAll(promptTokens);
        return !intentTokens.isEmpty();
    }

    private Set<String> keywordTokens(String text) {
        Set<String> stopWords = new HashSet<>(Arrays.asList(
                "the", "a", "an", "and", "or", "to", "for", "of", "in",
                "on", "with", "is", "are", "be", "as", "by", "at", "from",
                "this", "that", "it", "you", "your", "please"
        ));
        return Arrays.stream(text.toLowerCase()
                        .replaceAll("[^a-z0-9\\s]", " ")
                        .split("\\s+"))
                .filter(token -> !token.isBlank())
                .filter(token -> !stopWords.contains(token))
                .collect(Collectors.toSet());
    }

    public record ValidationResult(boolean valid, String reason, double reductionPct) {}
}