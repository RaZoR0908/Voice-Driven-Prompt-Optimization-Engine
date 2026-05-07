package com.razer.engine.pipeline;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.razer.engine.dto.IntentResponseDTO;
import com.razer.engine.dto.VoiceInputDTO;
import com.razer.engine.filter.AbuseGuardFilter;
import com.razer.engine.filter.FillerWordFilter;
import com.razer.engine.filter.LanguageNormalizer;
import com.razer.engine.model.Conversation;
import com.razer.engine.model.DecisionLog;
import com.razer.engine.model.Message;
import com.razer.engine.repository.ConversationRepository;
import com.razer.engine.repository.DecisionLogRepository;
import com.razer.engine.repository.MessageRepository;
import com.razer.engine.service.IntentDetectionService;
import com.razer.engine.service.SpeechToTextService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class VoicePipeline {

    private final SpeechToTextService speechToTextService;
    private final FillerWordFilter fillerWordFilter;
    private final LanguageNormalizer languageNormalizer;
    private final AbuseGuardFilter abuseGuardFilter;
    private final IntentDetectionService intentDetectionService;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final DecisionLogRepository decisionLogRepository;
    private final ObjectMapper objectMapper;

    public VoicePipeline(SpeechToTextService speechToTextService,
                         FillerWordFilter fillerWordFilter,
                         LanguageNormalizer languageNormalizer,
                         AbuseGuardFilter abuseGuardFilter,
                         IntentDetectionService intentDetectionService,
                         ConversationRepository conversationRepository,
                         MessageRepository messageRepository,
                         DecisionLogRepository decisionLogRepository,
                         ObjectMapper objectMapper) {
        this.speechToTextService = speechToTextService;
        this.fillerWordFilter = fillerWordFilter;
        this.languageNormalizer = languageNormalizer;
        this.abuseGuardFilter = abuseGuardFilter;
        this.intentDetectionService = intentDetectionService;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.decisionLogRepository = decisionLogRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public VoiceInputDTO process(String sessionId, MultipartFile audioFile, String text) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId is required");
        }

        // Get or create conversation
        Conversation conversation = conversationRepository.findBySessionId(sessionId)
                .orElseGet(() -> {
                    Conversation created = new Conversation();
                    created.setSessionId(sessionId);
                    return conversationRepository.save(created);
                });

        // STT or Text bypass
        String rawText;
        double confidence;

        if (text != null && !text.isBlank()) {
            // TEXT INPUT — skip Whisper entirely
            rawText = text;
            confidence = 1.0;
        } else {
            // AUDIO INPUT — run Whisper STT
            SpeechToTextService.TranscriptionResult transcription =
                    speechToTextService.transcribe(audioFile);
            rawText = transcription.text();
            confidence = transcription.confidence();
        }

        // Filter + normalize (runs for both text and audio)
        String filteredText = fillerWordFilter.clean(rawText);
        LanguageNormalizer.NormalizedText normalizedText =
                languageNormalizer.normalize(filteredText);
        abuseGuardFilter.guard(normalizedText.text());

        // Save message
        Message message = new Message();
        message.setConversation(conversation);
        message.setRawText(rawText);
        message.setTranscript(normalizedText.text());
        message.setConfidence(confidence);
        message.setLanguage(normalizedText.language());
        message = messageRepository.save(message);

        // Log STT step
        DecisionLog sttLog = new DecisionLog();
        sttLog.setMessage(message);
        sttLog.setStepName("STT");
        sttLog.setDecision("PASS");
        sttLog.setDetail(text != null && !text.isBlank()
                ? "Text input — Whisper bypassed"
                : "Audio transcribed with confidence=" + confidence);
        decisionLogRepository.save(sttLog);

        // Intent detection
        IntentResponseDTO intent =
                intentDetectionService.extractIntent(normalizedText.text());

        // Log intent step
        DecisionLog intentLog = new DecisionLog();
        intentLog.setMessage(message);
        intentLog.setStepName("INTENT");
        intentLog.setDecision("PASS");
        intentLog.setDetail("Intent extracted as " + intent.intent());
        decisionLogRepository.save(intentLog);

        String intentJson = toJson(intent);

        return new VoiceInputDTO(
                sessionId,
                message.getId(),
                rawText,
                normalizedText.text(),
                confidence,
                normalizedText.language(),
                intentJson
        );
    }

    private String toJson(IntentResponseDTO intent) {
        try {
            return objectMapper.writeValueAsString(intent);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to serialize intent JSON", exception);
        }
    }
}