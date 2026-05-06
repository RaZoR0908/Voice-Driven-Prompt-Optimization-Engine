package com.razer.engine.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.razer.engine.exception.LowConfidenceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Service
public class SpeechToTextService {

    private final WebClient webClient;
    private final String apiKey;
    private final String baseUrl;
    private final double confidenceThreshold;

    public SpeechToTextService(WebClient webClient,
                               @Value("${app.assemblyai.api-key:}") String apiKey,
                               @Value("${app.assemblyai.base-url:https://api.assemblyai.com/v2}") String baseUrl,
                               @Value("${app.validation.confidence-threshold:0.65}") double confidenceThreshold) {
        this.webClient = webClient;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.confidenceThreshold = confidenceThreshold;
    }

    public TranscriptionResult transcribe(MultipartFile audioFile) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("ASSEMBLYAI_API_KEY is required");
        }
        try {
            byte[] audioBytes = audioFile.getBytes();
            String uploadUrl = upload(audioBytes);
            JsonNode transcript = createTranscript(uploadUrl);
            String status = transcript.path("status").asText();
            while (!"completed".equalsIgnoreCase(status)) {
                if ("error".equalsIgnoreCase(status)) {
                    throw new IllegalStateException(transcript.path("error").asText("AssemblyAI transcription failed"));
                }
                if ("failed".equalsIgnoreCase(status)) {
                    throw new IllegalStateException("AssemblyAI transcription failed");
                }
                sleep(1200);
                transcript = fetchTranscript(transcript.path("id").asText());
                status = transcript.path("status").asText();
            }

            String text = transcript.path("text").asText("");
            double confidence = transcript.path("confidence").asDouble(0.0);
            String language = transcript.path("language_code").asText("en");

            if (confidence < confidenceThreshold) {
                throw new LowConfidenceException(confidence, "Speech confidence is below the required threshold");
            }

            return new TranscriptionResult(text, confidence, language);
        } catch (LowConfidenceException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to transcribe audio: " + exception.getMessage(), exception);
        }
    }

    private String upload(byte[] audioBytes) {
        JsonNode response = webClient.post()
                .uri(baseUrl + "/upload")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header("authorization", apiKey)
                .bodyValue(audioBytes)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block(Duration.ofSeconds(60));

        if (response == null || response.path("upload_url").isMissingNode()) {
            throw new IllegalStateException("AssemblyAI upload did not return an upload_url");
        }
        return response.path("upload_url").asText();
    }

    private JsonNode createTranscript(String uploadUrl) {
        JsonNode response = webClient.post()
                .uri(baseUrl + "/transcript")
                .contentType(MediaType.APPLICATION_JSON)
                .header("authorization", apiKey)
                .bodyValue(new TranscriptRequest(uploadUrl, true))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block(Duration.ofSeconds(60));

        if (response == null || response.path("id").isMissingNode()) {
            throw new IllegalStateException("AssemblyAI transcript creation failed");
        }
        return response;
    }

    private JsonNode fetchTranscript(String transcriptId) {
        JsonNode response = webClient.get()
                .uri(baseUrl + "/transcript/" + transcriptId)
                .header("authorization", apiKey)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block(Duration.ofSeconds(60));

        if (response == null) {
            throw new IllegalStateException("AssemblyAI transcript polling returned no payload");
        }
        return response;
    }

    private void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Transcription polling interrupted", exception);
        }
    }

    public record TranscriptionResult(String text, double confidence, String language) {
    }

    public record TranscriptRequest(String audio_url, boolean language_detection) {
    }
}