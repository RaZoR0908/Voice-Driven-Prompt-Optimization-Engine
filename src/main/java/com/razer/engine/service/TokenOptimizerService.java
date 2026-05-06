package com.razer.engine.service;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import org.springframework.stereotype.Service;

@Service
public class TokenOptimizerService {

    private final Encoding encoding;

    public TokenOptimizerService() {
        EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
        this.encoding = registry.getEncoding(EncodingType.CL100K_BASE);
    }

    public TokenStats calculate(String inputText, String optimizedText) {
        int inputTokens = countTokens(inputText);
        int outputTokens = countTokens(optimizedText);
        double reductionPct = inputTokens == 0 ? 0.0 : (1.0 - (double) outputTokens / (double) inputTokens) * 100.0;
        return new TokenStats(inputTokens, outputTokens, reductionPct);
    }

    public int countTokens(String text) {
        if (text == null || text.isBlank()) return 0;
        return encoding.countTokens(text);
    }

    public record TokenStats(int inputTokens, int outputTokens, double reductionPct) {
    }
}