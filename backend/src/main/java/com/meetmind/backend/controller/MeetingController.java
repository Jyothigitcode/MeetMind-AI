package com.meetmind.backend.controller;

import com.meetmind.backend.entity.Meeting;
import com.meetmind.backend.service.MeetingService;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/meetings")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://localhost:8082"
})
public class MeetingController
{
    private final MeetingService meetingService;

    public MeetingController(MeetingService meetingService)
    {
        this.meetingService = meetingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Meeting createMeeting(
            @RequestBody Meeting meeting)
    {
        return meetingService.saveMeeting(meeting);
    }

    @GetMapping("/{id}")
    public Meeting getMeeting(@PathVariable Long id)
    {
        return meetingService.getMeeting(id);
    }

    @GetMapping
    public List<Meeting> getAllMeetings()
    {
        return meetingService.getAllMeetings();
    }

    @PutMapping("/{id}")
    public Meeting updateMeeting(
            @PathVariable Long id,
            @RequestBody Meeting meeting)
    {
        return meetingService.updateMeeting(id, meeting);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMeeting(
            @PathVariable Long id)
    {
        meetingService.deleteMeeting(id);
    }

    @PostMapping("/upload")
    @ResponseStatus(HttpStatus.CREATED)
    public Meeting uploadMeeting(
            @RequestParam("title") String title,
            @RequestParam("file") MultipartFile file)
            throws IOException
    {
        return meetingService.uploadMeeting(title, file);
    }
}