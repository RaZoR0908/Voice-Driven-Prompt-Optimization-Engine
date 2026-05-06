package com.razer.engine.filter;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class AbuseGuardFilter {

    private static final Pattern BLOCKED_PATTERN = Pattern.compile(
            "(?i)\\b(hack|exploit|steal|phish|malware|ransomware|password dump|credit card|ddos|delete all data)\\b"
    );

    public void guard(String text) {
        if (!isAllowed(text)) {
            throw new IllegalArgumentException("Input is not an allowed task prompt");
        }
    }

    public boolean isAllowed(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String normalized = text.toLowerCase(Locale.ROOT);
        return !BLOCKED_PATTERN.matcher(normalized).find();
    }
}