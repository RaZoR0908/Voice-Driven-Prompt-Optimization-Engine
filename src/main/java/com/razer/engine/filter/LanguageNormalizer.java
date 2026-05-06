package com.razer.engine.filter;

import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class LanguageNormalizer {

    public NormalizedText normalize(String input) {
        String text = input == null ? "" : input.trim().replaceAll("\\s+", " ");
        String language = detectLanguage(text);
        String normalized = replaceCommonHinglish(text);
        return new NormalizedText(normalized, language);
    }

    public String detectLanguage(String input) {
        if (input == null || input.isBlank()) {
            return "en";
        }
        if (containsDevanagari(input)) {
            return "hi";
        }
        String lower = input.toLowerCase(Locale.ROOT);
        if (lower.matches(".*\\b(yaar|bhai|arrey|kya|kaise|kar do|bana do|thoda|please|plz)\\b.*")) {
            return "hinglish";
        }
        return "en";
    }

    private String replaceCommonHinglish(String text) {
        String normalized = text;
        normalized = normalized.replaceAll("(?i)\\b(please|plz)\\b", "");
        normalized = normalized.replaceAll("(?i)\\b(yaar|bhai|arrey)\\b", "");
        normalized = normalized.replaceAll("\\s+", " ").trim();
        return normalized;
    }

    private boolean containsDevanagari(String text) {
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            if (character >= '\u0900' && character <= '\u097F') {
                return true;
            }
        }
        return false;
    }

    public record NormalizedText(String text, String language) {
    }
}