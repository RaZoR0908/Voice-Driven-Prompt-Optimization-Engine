package com.razer.engine.controller;

import com.razer.engine.dto.ChatHistoryDTO;
import com.razer.engine.dto.DecisionLogDTO;
import com.razer.engine.model.Message;
import com.razer.engine.repository.MessageRepository;
import com.razer.engine.service.MemoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final MessageRepository messageRepository;
    private final MemoryService memoryService;

    public ChatController(MessageRepository messageRepository, MemoryService memoryService) {
        this.messageRepository = messageRepository;
        this.memoryService = memoryService;
    }

    @GetMapping("/chat/history")
    public ResponseEntity<List<ChatHistoryDTO>> history(@RequestParam("sessionId") String sessionId) {
        List<ChatHistoryDTO> history = messageRepository.findByConversation_SessionIdOrderByCreatedAtAsc(sessionId).stream()
                .map(this::toHistoryDto)
                .toList();
        return ResponseEntity.ok(history);
    }

    @GetMapping("/logs")
    public ResponseEntity<List<DecisionLogDTO>> logs(@RequestParam("messageId") UUID messageId) {
        return ResponseEntity.ok(memoryService.listLogs(messageId));
    }

    private ChatHistoryDTO toHistoryDto(Message message) {
        return new ChatHistoryDTO(
                message.getId(),
                message.getRawText(),
                message.getTranscript(),
                message.getOptimizedPrompt(),
                message.getTokenInput(),
                message.getTokenOutput(),
                message.getReductionPct(),
                message.getCreatedAt()
        );
    }
}