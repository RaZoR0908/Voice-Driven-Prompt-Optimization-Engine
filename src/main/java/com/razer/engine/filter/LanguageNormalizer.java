package com.razer.engine.filter;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Component
public class LanguageNormalizer {

    private static final Map<String, String> HINDI_TO_ENGLISH = new HashMap<>();

    static {

        HINDI_TO_ENGLISH.put("मार्केटिंग", "marketing");
        HINDI_TO_ENGLISH.put("प्लान", "plan");
        HINDI_TO_ENGLISH.put("बनाओ", "create");
        HINDI_TO_ENGLISH.put("शॉर्ट", "short");
        HINDI_TO_ENGLISH.put("ऐप", "app");
        HINDI_TO_ENGLISH.put("रणनीति", "strategy");
        HINDI_TO_ENGLISH.put("के लिए", "for");
        HINDI_TO_ENGLISH.put("अंदर", "under");
        HINDI_TO_ENGLISH.put("वर्ड्स", "words");
    }

    public NormalizedText normalize(String input) {

        String text = input == null
                ? ""
                : input.trim().replaceAll("\\s+", " ");

        String language = detectLanguage(text);

        String normalized = replaceCommonHinglish(text);

        if (!language.equals("en")) {
            normalized = translateToEnglish(normalized);
        }

        return new NormalizedText(
                normalized,
                language
        );
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

    private String translateToEnglish(String text) {

        String translated = text;

        for (Map.Entry<String, String> entry : HINDI_TO_ENGLISH.entrySet()) {

            translated = translated.replace(
                    entry.getKey(),
                    entry.getValue()
            );
        }

        return translated;
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

    public record NormalizedText(
            String text,
            String language
    ) {
    }
}