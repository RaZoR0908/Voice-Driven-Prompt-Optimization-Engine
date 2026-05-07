package com.razer.engine.controller;

import com.razer.engine.dto.MemoryCardDTO;
import com.razer.engine.memory.MemoryGraph;
import com.razer.engine.service.MemoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/memory")
public class MemoryController {

    private final MemoryService memoryService;

    public MemoryController(MemoryService memoryService) {
        this.memoryService = memoryService;
    }

    @GetMapping("/graph")
    public ResponseEntity<MemoryGraph.GraphPayload> graph() {
        return ResponseEntity.ok(memoryService.getGraph());
    }

    @GetMapping("/cards")
    public ResponseEntity<List<MemoryCardDTO>> cards(@RequestParam(required = false) String sessionId) {
        return ResponseEntity.ok(memoryService.listCards(sessionId));
    }
}