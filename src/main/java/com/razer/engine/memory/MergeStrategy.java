package com.razer.engine.memory;

import com.razer.engine.dto.IntentResponseDTO;
import com.razer.engine.model.Memory;
import org.springframework.stereotype.Component;

@Component
public class MergeStrategy {

    public Memory merge(Memory target, IntentResponseDTO intent, String optimizedPrompt) {
        if (target == null) {
            throw new IllegalArgumentException("Target memory is required for merge");
        }
        target.setDomain(intent.domain());
        target.setTask(intent.task());
        if (optimizedPrompt != null && !optimizedPrompt.isBlank()) {
            if (target.getOptimizedPrompt() == null || optimizedPrompt.length() <= target.getOptimizedPrompt().length()) {
                target.setOptimizedPrompt(optimizedPrompt);
            }
        }
        target.setUseCount((target.getUseCount() == null ? 0 : target.getUseCount()) + 1);
        return target;
    }
}