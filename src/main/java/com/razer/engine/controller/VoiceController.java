package com.razer.engine.controller;

import com.razer.engine.dto.VoiceInputDTO;
import com.razer.engine.pipeline.VoicePipeline;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/voice")
public class VoiceController {

    private final VoicePipeline voicePipeline;

    public VoiceController(VoicePipeline voicePipeline) {
        this.voicePipeline = voicePipeline;
    }

    @PostMapping(value = "/input", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VoiceInputDTO> input(@RequestPart("audio") MultipartFile audio,
                                               @RequestParam("sessionId") String sessionId) {
        return ResponseEntity.ok(voicePipeline.process(sessionId, audio));
    }
}