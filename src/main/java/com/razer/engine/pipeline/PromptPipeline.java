package com.razer.engine.pipeline;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.razer.engine.dto.IntentResponseDTO;
import com.razer.engine.dto.PromptResponseDTO;
import com.razer.engine.exception.IntentNotConfirmedException;
import com.razer.engine.model.DecisionLog;
import com.razer.engine.model.Message;
import com.razer.engine.model.PromptLog;
import com.razer.engine.repository.ConversationRepository;
import com.razer.engine.repository.DecisionLogRepository;
import com.razer.engine.repository.MessageRepository;
import com.razer.engine.repository.PromptLogRepository;
import com.razer.engine.service.MemoryService;
import com.razer.engine.service.PromptTransformService;
import com.razer.engine.service.TokenOptimizerService;
import com.razer.engine.service.ValidationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PromptPipeline {

    private final PromptTransformService promptTransformService;
    private final TokenOptimizerService tokenOptimizerService;
    private final ValidationService validationService;
    private final MemoryService memoryService;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final PromptLogRepository promptLogRepository;
    private final DecisionLogRepository decisionLogRepository;
    private final ObjectMapper objectMapper;

    public PromptPipeline(PromptTransformService promptTransformService,
                          TokenOptimizerService tokenOptimizerService,
                          ValidationService validationService,
                          MemoryService memoryService,
                          ConversationRepository conversationRepository,
                          MessageRepository messageRepository,
                          PromptLogRepository promptLogRepository,
                          DecisionLogRepository decisionLogRepository,
                          ObjectMapper objectMapper) {
        this.promptTransformService = promptTransformService;
        this.tokenOptimizerService = tokenOptimizerService;
        this.validationService = validationService;
        this.memoryService = memoryService;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.promptLogRepository = promptLogRepository;
        this.decisionLogRepository = decisionLogRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public PromptResponseDTO generate(IntentResponseDTO intent, String sessionId) {
        if (intent == null) {
            throw new IntentNotConfirmedException("Intent confirmation is required before prompt generation");
        }

        // Resolve message first so we can log against it
        Message message = resolveMessage(sessionId, intent);

        // ✅ Log CONFIRM=PASS — user reached this endpoint means they confirmed
        DecisionLog confirmLog = new DecisionLog();
        confirmLog.setMessage(message);
        confirmLog.setStepName("CONFIRM");
        confirmLog.setDecision("PASS");
        confirmLog.setDetail("User confirmed intent: " + intent.intent());
        decisionLogRepository.save(confirmLog);

        // ✅ Use intent.task() as source text — not full JSON
        String sourceText = intent.task() != null && !intent.task().isBlank()
                ? intent.task()
                : toJson(intent);

        String optimizedPrompt = promptTransformService.optimize(intent);
        TokenOptimizerService.TokenStats stats = tokenOptimizerService.calculate(sourceText, optimizedPrompt);
        ValidationService.ValidationResult validationResult = validationService.validate(intent, sourceText, optimizedPrompt);

        if (!validationResult.valid()) {
            DecisionLog failedValidation = new DecisionLog();
            failedValidation.setMessage(message);
            failedValidation.setStepName("VALIDATE");
            failedValidation.setDecision("FAIL");
            failedValidation.setDetail(validationResult.reason());
            decisionLogRepository.save(failedValidation);

            // Retry once
            optimizedPrompt = promptTransformService.optimize(intent);
            stats = tokenOptimizerService.calculate(sourceText, optimizedPrompt);
            validationResult = validationService.validate(intent, sourceText, optimizedPrompt);

            if (!validationResult.valid()) {
                throw new IllegalStateException(validationResult.reason());
            }
        }

        // Log validation pass
        DecisionLog passedValidation = new DecisionLog();
        passedValidation.setMessage(message);
        passedValidation.setStepName("VALIDATION");
        passedValidation.setDecision("COMPLETED");
        passedValidation.setDetail("Token reduction: " + String.format("%.0f", stats.reductionPct()) + "%");
        decisionLogRepository.save(passedValidation);

        // Update message
        message.setOptimizedPrompt(optimizedPrompt);
        message.setTokenInput(stats.inputTokens());
        message.setTokenOutput(stats.outputTokens());
        message.setReductionPct(stats.reductionPct());
        message = messageRepository.save(message);

        // Save prompt log
        PromptLog promptLog = new PromptLog();
        promptLog.setMessage(message);
        promptLog.setRawText(sourceText);
        promptLog.setOptimizedPrompt(optimizedPrompt);
        promptLog.setTokenInput(stats.inputTokens());
        promptLog.setTokenOutput(stats.outputTokens());
        promptLog.setReductionPct(stats.reductionPct());
        promptLogRepository.save(promptLog);

        // Log transform
        DecisionLog transformLog = new DecisionLog();
        transformLog.setMessage(message);
        transformLog.setStepName("TRANSFORM");
        transformLog.setDecision("COMPLETED");
        transformLog.setDetail("CAVEMAN MODE applied");
        decisionLogRepository.save(transformLog);

        // Memory
        MemoryService.MemoryDecisionResult memoryDecision = 
                memoryService.recordMemory(message, intent, optimizedPrompt);

        return new PromptResponseDTO(
                optimizedPrompt,
                stats.inputTokens(),
                stats.outputTokens(),
                stats.reductionPct(),
                memoryDecision.decision(),
                message.getId(),
                memoryDecision.memory() == null ? null : memoryDecision.memory().getId()
        );
    }

    private Message resolveMessage(String sessionId, IntentResponseDTO intent) {
        if (sessionId != null && !sessionId.isBlank()) {
            return messageRepository
                    .findFirstByConversation_SessionIdOrderByCreatedAtDesc(sessionId)
                    .orElseGet(() -> createOrphanMessage(intent));
        }
        return createOrphanMessage(intent);
    }

    private Message createOrphanMessage(IntentResponseDTO intent) {
        Message message = new Message();
        message.setRawText(intent.task());
        message.setTranscript(intent.task());
        message.setLanguage("en");
        message.setConfidence(1.0);
        return messageRepository.save(message);
    }

    private String toJson(IntentResponseDTO intent) {
        try {
            return objectMapper.writeValueAsString(intent);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to serialize intent JSON", exception);
        }
    }
}