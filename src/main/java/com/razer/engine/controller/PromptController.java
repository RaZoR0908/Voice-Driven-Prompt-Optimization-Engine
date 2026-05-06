package com.razer.engine.controller;

import com.razer.engine.dto.IntentResponseDTO;
import com.razer.engine.dto.PromptResponseDTO;
import com.razer.engine.pipeline.PromptPipeline;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/prompt")
public class PromptController {

    private final PromptPipeline promptPipeline;

    public PromptController(PromptPipeline promptPipeline) {
        this.promptPipeline = promptPipeline;
    }

    @PostMapping("/generate")
    public ResponseEntity<PromptResponseDTO> generate(@Valid @RequestBody IntentResponseDTO intent,
                                                      @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {
        return ResponseEntity.ok(promptPipeline.generate(intent, sessionId));
    }
}