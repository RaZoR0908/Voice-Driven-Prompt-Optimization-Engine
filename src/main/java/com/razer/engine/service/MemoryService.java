package com.razer.engine.service;

import com.razer.engine.dto.DecisionLogDTO;
import com.razer.engine.dto.IntentResponseDTO;
import com.razer.engine.dto.MemoryCardDTO;
import com.razer.engine.memory.MemoryAllocator;
import com.razer.engine.memory.MemoryGraph;
import com.razer.engine.memory.MergeStrategy;
import com.razer.engine.model.DecisionLog;
import com.razer.engine.model.Memory;
import com.razer.engine.model.MemoryEdge;
import com.razer.engine.model.Message;
import com.razer.engine.repository.DecisionLogRepository;
import com.razer.engine.repository.MemoryEdgeRepository;
import com.razer.engine.repository.MemoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class MemoryService {

    private final MemoryRepository memoryRepository;
    private final MemoryEdgeRepository memoryEdgeRepository;
    private final DecisionLogRepository decisionLogRepository;
    private final MemoryAllocator memoryAllocator;
    private final MergeStrategy mergeStrategy;
    private final MemoryGraph memoryGraph;

    public MemoryService(MemoryRepository memoryRepository,
                         MemoryEdgeRepository memoryEdgeRepository,
                         DecisionLogRepository decisionLogRepository,
                         MemoryAllocator memoryAllocator,
                         MergeStrategy mergeStrategy,
                         MemoryGraph memoryGraph) {
        this.memoryRepository = memoryRepository;
        this.memoryEdgeRepository = memoryEdgeRepository;
        this.decisionLogRepository = decisionLogRepository;
        this.memoryAllocator = memoryAllocator;
        this.mergeStrategy = mergeStrategy;
        this.memoryGraph = memoryGraph;
    }

    @Transactional
    public MemoryDecisionResult recordMemory(Message message, IntentResponseDTO intent, String optimizedPrompt) {
        List<Memory> memories = memoryRepository.findAllByOrderByUpdatedAtDesc();
        MemoryAllocator.AllocationResult allocation = memoryAllocator.allocate(intent, optimizedPrompt, memories);
        Memory chosenMemory = null;
        String sessionId = message.getConversation() != null ? message.getConversation().getSessionId() : null;

        switch (allocation.decision()) {
            case MERGE -> {
                chosenMemory = mergeStrategy.merge(allocation.relatedMemory(), intent, optimizedPrompt);
                memoryRepository.save(chosenMemory);
            }
            case SAVE_CHILD, SAVE_NEW -> {
                Memory memory = new Memory();
                memory.setSessionId(sessionId);
                memory.setDomain(intent.domain());
                memory.setTask(intent.task());
                memory.setOptimizedPrompt(optimizedPrompt);
                memory.setUseCount(1);
                chosenMemory = memoryRepository.save(memory);

                if (allocation.relatedMemory() != null) {
                    MemoryEdge edge = new MemoryEdge();
                    edge.setFromMemory(allocation.relatedMemory());
                    edge.setToMemory(chosenMemory);
                    edge.setEdgeType(allocation.decision() == MemoryAllocator.Decision.SAVE_CHILD ? "REFINES" : "RELATED");
                    edge.setScore(allocation.similarityScore());
                    memoryEdgeRepository.save(edge);
                }
            }
            case SKIP -> {
                chosenMemory = null;
            }
        }

        DecisionLog decisionLog = new DecisionLog();
        decisionLog.setMessage(message);
        decisionLog.setStepName("MEMORY");
        decisionLog.setDecision(allocation.decision().name());
        decisionLog.setDetail("Similarity score=" + allocation.similarityScore());
        decisionLogRepository.save(decisionLog);

        return new MemoryDecisionResult(allocation.decision().name(), allocation.similarityScore(), chosenMemory);
    }

    public List<MemoryCardDTO> listCards(String sessionId) {
        List<Memory> memories;
        if (sessionId != null && !sessionId.isEmpty()) {
            memories = memoryRepository.findBySessionIdOrderByUpdatedAtDesc(sessionId);
        } else {
            memories = memoryRepository.findAllByOrderByUpdatedAtDesc();
        }
        return memories.stream()
                .map(memory -> new MemoryCardDTO(memory.getId(), memory.getDomain(), memory.getTask(), memory.getOptimizedPrompt(), safeInt(memory.getUseCount()), memory.getUpdatedAt()))
                .toList();
    }

    public MemoryGraph.GraphPayload getGraph() {
        return memoryGraph.build(memoryRepository.findAllByOrderByUpdatedAtDesc(), memoryEdgeRepository.findAll());
    }

    public List<DecisionLogDTO> listLogs(UUID messageId) {
        return decisionLogRepository.findByMessage_IdOrderByCreatedAtAsc(messageId).stream()
                .map(log -> new DecisionLogDTO(messageId, log.getStepName(), log.getDecision(), log.getDetail(), log.getCreatedAt()))
                .toList();
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    public record MemoryDecisionResult(String decision, double similarityScore, Memory memory) {
    }
}