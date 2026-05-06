package com.razer.engine.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/")
    public Map<String, String> health() {

        return Map.of(
                "status", "RUNNING",
                "service", "Voice Driven Prompt Optimization Engine"
        );
    }
}