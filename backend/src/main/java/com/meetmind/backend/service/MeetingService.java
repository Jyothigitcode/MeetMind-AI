package com.meetmind.backend.service;

import com.meetmind.backend.entity.Meeting;
import com.meetmind.backend.repository.MeetingRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
public class MeetingService
{
    private final MeetingRepository meetingRepository;

    private final AiServiceClient aiServiceClient;

    private final Path uploadDirectory = Paths.get("uploads");

    public MeetingService(
            MeetingRepository meetingRepository,
            AiServiceClient aiServiceClient)
    {
        this.meetingRepository = meetingRepository;
        this.aiServiceClient = aiServiceClient;
    }

    public Meeting saveMeeting(Meeting meeting)
    {
        return meetingRepository.save(meeting);
    }

    public List<Meeting> getAllMeetings()
    {
        return meetingRepository.findAll();
    }

    public Meeting updateMeeting(Long id, Meeting updatedMeeting)
    {
        Meeting existingMeeting = meetingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Meeting not found"));

        existingMeeting.setTitle(updatedMeeting.getTitle());
        existingMeeting.setTranscript(updatedMeeting.getTranscript());
        existingMeeting.setSummary(updatedMeeting.getSummary());
        existingMeeting.setKeyDecisions(updatedMeeting.getKeyDecisions());
        existingMeeting.setActionItems(updatedMeeting.getActionItems());

        return meetingRepository.save(existingMeeting);
    }

    public void deleteMeeting(Long id)
    {
        Meeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Meeting not found"));

        if (meeting.getFileName() != null)
        {
            try
            {
                Files.deleteIfExists(
                        uploadDirectory.resolve(meeting.getFileName())
                );
            }
            catch (IOException exception)
            {
                throw new RuntimeException(
                        "Unable to delete meeting file",
                        exception
                );
            }
        }

        meetingRepository.delete(meeting);
    }

    public Meeting uploadMeeting(
            String title,
            MultipartFile file) throws IOException
    {
        if (file.isEmpty())
        {
            throw new IllegalArgumentException(
                    "Please select an audio or video file"
            );
        }

        String contentType = file.getContentType();

        if (contentType == null ||
                (!contentType.startsWith("audio/") &&
                 !contentType.startsWith("video/")))
        {
            throw new IllegalArgumentException(
                    "Only audio and video files are supported"
            );
        }

        Files.createDirectories(uploadDirectory);

        String originalFileName = file.getOriginalFilename();

        String extension = "";

        if (originalFileName != null &&
                originalFileName.contains("."))
        {
            extension = originalFileName.substring(
                    originalFileName.lastIndexOf(".")
            );
        }

        String storedFileName =
                UUID.randomUUID() + extension;

        Path destination =
                uploadDirectory.resolve(storedFileName);

        Files.copy(
                file.getInputStream(),
                destination,
                StandardCopyOption.REPLACE_EXISTING
        );

        Meeting meeting = new Meeting();

        meeting.setTitle(title);
        meeting.setFileName(storedFileName);
        meeting.setOriginalFileName(originalFileName);
        meeting.setFileType(contentType);
        meeting.setFileSize(file.getSize());

        // File uploaded successfully.
        meeting.setStatus("UPLOADED");

        // Save meeting first.
        meeting = meetingRepository.save(meeting);

        try
        {
            // Convert Path into File.
            File audioFile = destination.toFile();

            // Send file to Python AI service for transcription.
            String transcript =
                    aiServiceClient.transcribe(audioFile);

            // Store transcript.
            meeting.setTranscript(transcript);

            // Update status.
            meeting.setStatus("TRANSCRIBED");

            // Save transcript into PostgreSQL.
            meeting = meetingRepository.save(meeting);


            // Generate the summary, decisions, and action items
            // from the completed transcript.
            MeetingAnalysis analysis =
                    aiServiceClient.analyze(transcript);

            meeting.setSummary(analysis.summary());
            meeting.setKeyDecisions(analysis.keyDecisions());
            meeting.setActionItems(analysis.actionItems());

            // Update final status.
            meeting.setStatus("COMPLETED");

            // Save the complete meeting analysis into PostgreSQL.
            meeting = meetingRepository.save(meeting);
        }
        catch (Exception exception)
        {
            meeting.setStatus("AI_PROCESSING_FAILED");

            meetingRepository.save(meeting);

            throw new RuntimeException(
                    "AI processing failed",
                    exception
            );
        }

        return meeting;
    }
}
