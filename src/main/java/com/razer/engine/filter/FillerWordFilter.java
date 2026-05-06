package com.razer.engine.filter;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class FillerWordFilter {

    private static final Pattern FILLER_PATTERN = Pattern.compile(
            "(?i)\\b(um+|uh+|erm+|like|basically|actually|you know|sort of|kind of|yaar+|bhai|arrey|मतलब)\\b"
    );

    public String clean(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String cleaned = FILLER_PATTERN.matcher(input).replaceAll(" ");
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        return cleaned;
    }
}