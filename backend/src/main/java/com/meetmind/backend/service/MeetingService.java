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
public class MeetingService {
        private final MeetingRepository meetingRepository;

        private final MeetingProcessingService meetingProcessingService;

        private final Path uploadDirectory = Paths.get("uploads");

        public MeetingService(
                        MeetingRepository meetingRepository,
                        MeetingProcessingService meetingProcessingService) {
                this.meetingRepository = meetingRepository;
                this.meetingProcessingService = meetingProcessingService;
        }

        public Meeting saveMeeting(Meeting meeting) {
                return meetingRepository.save(meeting);
        }

        public List<Meeting> getAllMeetings() {
                return meetingRepository.findAll();
        }

        public Meeting getMeeting(Long id) {
                return meetingRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Meeting not found"));
        }

        public Meeting updateMeeting(
                        Long id,
                        Meeting updatedMeeting) {
                Meeting existingMeeting = meetingRepository
                                .findById(id)
                                .orElseThrow(
                                                () -> new RuntimeException(
                                                                "Meeting not found"));

                existingMeeting.setTitle(
                                updatedMeeting.getTitle());

                existingMeeting.setTranscript(
                                updatedMeeting.getTranscript());

                existingMeeting.setSummary(
                                updatedMeeting.getSummary());

                existingMeeting.setKeyDecisions(
                                updatedMeeting.getKeyDecisions());

                existingMeeting.setActionItems(
                                updatedMeeting.getActionItems());

                return meetingRepository.save(
                                existingMeeting);
        }

        public void deleteMeeting(Long id) {
                Meeting meeting = meetingRepository
                                .findById(id)
                                .orElseThrow(
                                                () -> new RuntimeException(
                                                                "Meeting not found"));

                if (meeting.getFileName() != null) {
                        try {
                                Files.deleteIfExists(
                                                uploadDirectory.resolve(
                                                                meeting.getFileName()));
                        } catch (IOException exception) {
                                throw new RuntimeException(
                                                "Unable to delete meeting file",
                                                exception);
                        }
                }

                meetingRepository.delete(meeting);
        }

        public Meeting uploadMeeting(
                        String title,
                        MultipartFile file)
                        throws IOException {
                validateUploadedFile(file);

                Files.createDirectories(
                                uploadDirectory);

                String originalFileName = file.getOriginalFilename();

                String extension = getFileExtension(
                                originalFileName);

                String storedFileName = UUID.randomUUID()
                                + extension;

                Path destination = uploadDirectory.resolve(
                                storedFileName);

                Files.copy(
                                file.getInputStream(),
                                destination,
                                StandardCopyOption.REPLACE_EXISTING);

                Meeting meeting = createMeetingRecord(
                                title,
                                file,
                                originalFileName,
                                storedFileName);

                meeting = meetingRepository.save(
                                meeting);

                meeting.setStatus("PROCESSING");
                meeting.setProcessingStage("QUEUED");
                meeting.setProgressPercent(5);
                meeting = meetingRepository.save(meeting);

                meetingProcessingService.processMeeting(
                                meeting.getId(),
                                storedFileName);

                return meeting;
        }

        private void validateUploadedFile(MultipartFile file) {
                if (file.isEmpty())
                        throw new IllegalArgumentException("Please select an audio or video file");

                String contentType = file.getContentType();
                String fileName = file.getOriginalFilename();

                boolean validMimeType = contentType != null &&
                                (contentType.startsWith("audio/") || contentType.startsWith("video/"));

                boolean validExtension = fileName != null &&
                                fileName.toLowerCase().matches(
                                                ".*\\.(mp3|wav|m4a|aac|ogg|flac|mp4|mpeg|mpg|mov|avi|mkv|webm)$");

                if (!validMimeType && !validExtension)
                        throw new IllegalArgumentException("Only audio and video files are supported");
        }

        private String getFileExtension(
                        String originalFileName) {
                if (originalFileName != null
                                && originalFileName.contains(".")) {
                        return originalFileName.substring(
                                        originalFileName.lastIndexOf("."));
                }

                return "";
        }

        private Meeting createMeetingRecord(
                        String title,
                        MultipartFile file,
                        String originalFileName,
                        String storedFileName) {
                Meeting meeting = new Meeting();

                meeting.setTitle(
                                title);

                meeting.setFileName(
                                storedFileName);

                meeting.setOriginalFileName(
                                originalFileName);

                meeting.setFileType(
                                file.getContentType());

                meeting.setFileSize(
                                file.getSize());

                meeting.setStatus(
                                "UPLOADED");

                return meeting;
        }

}
