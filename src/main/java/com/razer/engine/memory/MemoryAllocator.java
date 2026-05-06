package com.razer.engine.memory;

import com.razer.engine.dto.IntentResponseDTO;
import com.razer.engine.model.Memory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class MemoryAllocator {

    public enum Decision {
        MERGE,
        SAVE_CHILD,
        SAVE_NEW,
        SKIP
    }

    private final double mergeThreshold;
    private final double childThreshold;

    public MemoryAllocator(@Value("${app.memory.merge-threshold:0.85}") double mergeThreshold,
                           @Value("${app.memory.child-threshold:0.50}") double childThreshold) {
        this.mergeThreshold = mergeThreshold;
        this.childThreshold = childThreshold;
    }

    public AllocationResult allocate(IntentResponseDTO intent, String optimizedPrompt, List<Memory> memories) {
        if (intent == null || optimizedPrompt == null || optimizedPrompt.isBlank()) {
            return new AllocationResult(Decision.SKIP, 0.0, null);
        }
        if (memories == null || memories.isEmpty()) {
            return new AllocationResult(Decision.SAVE_NEW, 0.0, null);
        }

        Memory bestMatch = memories.stream()
                .max(Comparator.comparingDouble(memory -> similarity(intent, optimizedPrompt, memory)))
                .orElse(null);

        if (bestMatch == null) {
            return new AllocationResult(Decision.SAVE_NEW, 0.0, null);
        }

        double score = similarity(intent, optimizedPrompt, bestMatch);
        if (score >= mergeThreshold) {
            return new AllocationResult(Decision.MERGE, score, bestMatch);
        }
        if (score >= childThreshold) {
            return new AllocationResult(Decision.SAVE_CHILD, score, bestMatch);
        }
        return new AllocationResult(Decision.SAVE_NEW, score, bestMatch);
    }

    private double similarity(IntentResponseDTO intent, String optimizedPrompt, Memory memory) {
        Set<String> left = tokens(intent.task() + " " + intent.domain() + " " + String.join(" ", intent.constraints()) + " " + optimizedPrompt);
        Set<String> right = tokens(memory.getTask() + " " + memory.getDomain() + " " + memory.getOptimizedPrompt());
        if (left.isEmpty() || right.isEmpty()) {
            return 0.0;
        }

        Set<String> intersection = new HashSet<>(left);
        intersection.retainAll(right);

        int shared = intersection.size();
        double jaccard = (double) shared / (double) (left.size() + right.size() - shared);
        double overlap = (2.0 * shared) / (double) (left.size() + right.size());
        return Math.min(1.0, (jaccard * 0.65) + (overlap * 0.35));
    }

    private Set<String> tokens(String text) {
        String normalized = text == null ? "" : text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9\\s]", " ");
        String[] split = normalized.split("\\s+");
        Set<String> stopWords = Set.of("the", "a", "an", "and", "or", "to", "for", "of", "in", "on", "with", "is", "are", "be", "as", "by", "at", "from", "this", "that", "it", "you", "your", "please", "make", "create");
        Set<String> tokens = new HashSet<>();
        for (String token : split) {
            if (!token.isBlank() && !stopWords.contains(token)) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    public record AllocationResult(Decision decision, double similarityScore, Memory relatedMemory) {
    }
}