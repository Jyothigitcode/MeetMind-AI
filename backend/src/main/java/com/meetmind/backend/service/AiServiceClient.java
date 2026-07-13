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

    public String transcribe(File audioFile)
    {
        try
        {
            FileSystemResource fileResource =
                    new FileSystemResource(audioFile);

            HttpHeaders fileHeaders = new HttpHeaders();
            fileHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            fileHeaders.setContentDispositionFormData(
                    "file",
                    audioFile.getName()
            );

            HttpEntity<FileSystemResource> fileEntity =
                    new HttpEntity<>(fileResource, fileHeaders);

            MultiValueMap<String, Object> body =
                    new LinkedMultiValueMap<>();
            body.add("file", fileEntity);

            HttpHeaders requestHeaders = new HttpHeaders();
            requestHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);

            ResponseEntity<String> response =
                    restTemplate.postForEntity(
                            aiServiceUrl + "/transcribe",
                            new HttpEntity<>(body, requestHeaders),
                            String.class
                    );

            JsonNode transcript = objectMapper
                    .readTree(response.getBody())
                    .path("transcript");

            if (!transcript.isTextual() || transcript.asText().isBlank())
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

    public MeetingAnalysis analyze(String transcript)
    {
        try
        {
            HttpHeaders requestHeaders = new HttpHeaders();
            requestHeaders.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<String> response =
                    restTemplate.postForEntity(
                            aiServiceUrl + "/summarize",
                            new HttpEntity<>(
                                    Map.of("transcript", transcript),
                                    requestHeaders
                            ),
                            String.class
                    );

            JsonNode analysis = objectMapper.readTree(response.getBody());
            JsonNode summary = analysis.path("summary");

            if (!summary.isTextual() || summary.asText().isBlank())
            {
                throw new RuntimeException(
                        "AI service returned no summary"
                );
            }

            return new MeetingAnalysis(
                    summary.asText(),
                    jsonArray(analysis, "keyDecisions"),
                    jsonArray(analysis, "actionItems")
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

    private String jsonArray(
            JsonNode analysis,
            String fieldName) throws Exception
    {
        JsonNode value = analysis.path(fieldName);

        if (!value.isArray())
        {
            value = objectMapper.createArrayNode();
        }

        return objectMapper.writeValueAsString(value);
    }
}
