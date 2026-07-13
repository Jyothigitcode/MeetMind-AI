package com.meetmind.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.util.Map;


@Service
public class AiServiceClient
{
    private final String aiServiceUrl;

    private final ObjectMapper objectMapper;

    private final RestTemplate restTemplate;


    public AiServiceClient(
            @Value("${ai.service.url}") String aiServiceUrl,
            ObjectMapper objectMapper)
    {
        this.aiServiceUrl = aiServiceUrl;

        this.objectMapper = objectMapper;

        this.restTemplate = new RestTemplate();
    }


    // ============================================================
    // TRANSCRIPTION
    // ============================================================

    public String transcribe(File audioFile)
    {
        try
        {
            FileSystemResource fileResource =
                    new FileSystemResource(audioFile);


            HttpHeaders fileHeaders =
                    new HttpHeaders();

            fileHeaders.setContentType(
                    MediaType.APPLICATION_OCTET_STREAM
            );

            fileHeaders.setContentDispositionFormData(
                    "file",
                    audioFile.getName()
            );


            HttpEntity<FileSystemResource> fileEntity =
                    new HttpEntity<>(
                            fileResource,
                            fileHeaders
                    );


            MultiValueMap<String, Object> body =
                    new LinkedMultiValueMap<>();

            body.add(
                    "file",
                    fileEntity
            );


            HttpHeaders requestHeaders =
                    new HttpHeaders();

            requestHeaders.setContentType(
                    MediaType.MULTIPART_FORM_DATA
            );


            HttpEntity<MultiValueMap<String, Object>>
                    requestEntity =
                    new HttpEntity<>(
                            body,
                            requestHeaders
                    );


            ResponseEntity<String> response =
                    restTemplate.postForEntity(
                            aiServiceUrl + "/transcribe",
                            requestEntity,
                            String.class
                    );


            JsonNode responseJson =
                    objectMapper.readTree(
                            response.getBody()
                    );


            JsonNode transcript =
                    responseJson.path("transcript");


            if (!transcript.isTextual()
                    || transcript.asText().isBlank())
            {
                throw new RuntimeException(
                        "AI service returned no transcript"
                );
            }


            return transcript.asText();
        }
        catch (Exception exception)
        {
            throw new RuntimeException(
                    "AI transcription failed",
                    exception
            );
        }
    }


    // ============================================================
    // MEETING ANALYSIS
    // ============================================================

    public MeetingAnalysis analyze(String transcript)
    {
        try
        {
            HttpHeaders requestHeaders =
                    new HttpHeaders();

            requestHeaders.setContentType(
                    MediaType.APPLICATION_JSON
            );


            Map<String, String> body =
                    Map.of(
                            "transcript",
                            transcript
                    );


            HttpEntity<Map<String, String>>
                    requestEntity =
                    new HttpEntity<>(
                            body,
                            requestHeaders
                    );


            ResponseEntity<String> response =
                    restTemplate.postForEntity(
                            aiServiceUrl + "/summarize",
                            requestEntity,
                            String.class
                    );


            JsonNode analysis =
                    objectMapper.readTree(
                            response.getBody()
                    );


            JsonNode summary =
                    analysis.path("summary");


            if (!summary.isTextual()
                    || summary.asText().isBlank())
            {
                throw new RuntimeException(
                        "AI service returned no summary"
                );
            }


            return objectMapper.treeToValue(
                    analysis,
                    MeetingAnalysis.class
            );
        }
        catch (Exception exception)
        {
            throw new RuntimeException(
                    "AI meeting analysis failed",
                    exception
            );
        }
    }
}