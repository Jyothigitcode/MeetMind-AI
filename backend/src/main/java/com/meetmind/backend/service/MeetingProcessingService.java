package com.meetmind.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetmind.backend.entity.Meeting;
import com.meetmind.backend.repository.MeetingRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class MeetingProcessingService {
    private final MeetingRepository meetingRepository;
    private final AiServiceClient aiServiceClient;
    private final ObjectMapper objectMapper;
    private final Path uploadDirectory = Paths.get("uploads");

    public MeetingProcessingService(MeetingRepository meetingRepository,
                                    AiServiceClient aiServiceClient,
                                    ObjectMapper objectMapper) {
        this.meetingRepository = meetingRepository;
        this.aiServiceClient = aiServiceClient;
        this.objectMapper = objectMapper;
    }

    @Async
    public void processMeeting(Long meetingId, String storedFileName) {
        Meeting meeting = meetingRepository.findById(meetingId).orElse(null);
        if (meeting == null) return;

        try {
            meeting.setStatus("PROCESSING");
            meeting.setProcessingStage("TRANSCRIPTION");
            meeting.setProgressPercent(15);
            meeting.setProcessingError(null);
            meetingRepository.save(meeting);

            String transcript = aiServiceClient.transcribe(
                    uploadDirectory.resolve(storedFileName).toFile());
            if (transcript == null || transcript.isBlank()) {
                throw new IllegalStateException("Transcription returned empty text.");
            }

            meeting.setTranscript(transcript);
            meeting.setStatus("PROCESSING");
            meeting.setProcessingStage("AI_ANALYSIS");
            meeting.setProgressPercent(60);
            meetingRepository.save(meeting);

            MeetingAnalysis analysis = aiServiceClient.analyze(transcript);
            meeting.setSummary(analysis.summary());
            meeting.setKeyDecisions(objectMapper.writeValueAsString(analysis.keyDecisions()));
            meeting.setActionItems(objectMapper.writeValueAsString(analysis.actionItems()));

            meeting.setProcessingStage("SAVING_RESULTS");
            meeting.setProgressPercent(90);
            meetingRepository.save(meeting);

            meeting.setStatus("COMPLETED");
            meeting.setProcessingStage("COMPLETED");
            meeting.setProgressPercent(100);
            meeting.setProcessingError(null);
            meetingRepository.save(meeting);
        } catch (Exception exception) {
            meeting.setStatus("AI_PROCESSING_FAILED");
            meeting.setProcessingStage("FAILED");
            meeting.setProcessingError(safeMessage(exception));
            meetingRepository.save(meeting);
        }
    }

    private String safeMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) return "Processing failed. Check backend and AI service logs.";
        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}
