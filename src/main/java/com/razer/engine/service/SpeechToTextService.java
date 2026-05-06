package com.razer.engine.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.razer.engine.exception.LowConfidenceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Service
public class SpeechToTextService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String whisperModel;
    private final double confidenceThreshold;

    public SpeechToTextService(WebClient webClient,
                               ObjectMapper objectMapper,
                               @Value("${groq.api-key}") String apiKey,
                               @Value("${groq.whisper-model:whisper-large-v3}") String whisperModel,
                               @Value("${app.token.confidence-threshold:0.65}") double confidenceThreshold) {

        this.webClient = webClient;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.whisperModel = whisperModel;
        this.confidenceThreshold = confidenceThreshold;
    }

    public TranscriptionResult transcribe(MultipartFile audioFile) {

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("GROQ_API_KEY is required");
        }

        try {

            byte[] audioBytes = audioFile.getBytes();

            String originalFilename = audioFile.getOriginalFilename() != null
                    ? audioFile.getOriginalFilename()
                    : "audio.webm";

            MultipartBodyBuilder builder = new MultipartBodyBuilder();

            builder.part("file", new ByteArrayResource(audioBytes) {
                @Override
                public String getFilename() {
                    return originalFilename;
                }
            });

            builder.part("model", whisperModel);
            builder.part("response_format", "verbose_json");

            String responseBody = webClient.post()
                    .uri("https://api.groq.com/openai/v1/audio/transcriptions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .onStatus(
                            status -> status.isError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .map(body -> new RuntimeException("Groq Whisper Error: " + body))
                    )
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(60));

            JsonNode response;

            try {
                response = objectMapper.readTree(responseBody);
            } catch (Exception exception) {
                throw new IllegalStateException(
                        "Unable to parse Groq Whisper response",
                        exception
                );
            }

            if (response == null || response.path("text").isMissingNode()) {
                throw new IllegalStateException("Groq Whisper returned no transcription");
            }

            String text = response.path("text").asText("").trim();

            String language = response.path("language").asText("en");

            double confidence = extractConfidence(response);

            if (confidence < confidenceThreshold) {
                throw new LowConfidenceException(
                        confidence,
                        "Speech confidence is below threshold"
                );
            }

            return new TranscriptionResult(
                    text,
                    confidence,
                    language
            );

        } catch (LowConfidenceException exception) {
            throw exception;

        } catch (Exception exception) {

            throw new IllegalStateException(
                    "Unable to transcribe audio: " + exception.getMessage(),
                    exception
            );
        }
    }

    private double extractConfidence(JsonNode response) {

        JsonNode segments = response.path("segments");

        if (segments.isArray() && segments.size() > 0) {

            double totalLogProb = 0.0;
            int count = 0;

            for (JsonNode segment : segments) {

                if (!segment.path("avg_logprob").isMissingNode()) {

                    totalLogProb += segment.path("avg_logprob").asDouble();

                    count++;
                }
            }

            if (count > 0) {

                double avgLogProb = totalLogProb / count;

                return Math.min(1.0, Math.exp(avgLogProb));
            }
        }

        return 0.85;
    }

    public record TranscriptionResult(
            String text,
            double confidence,
            String language
    ) {}
}