package com.meetmind.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Meeting
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String transcript;

    @Column(columnDefinition = "TEXT")
    private String transcriptSegments;

    private Double transcriptionSeconds;

    private Double analysisSeconds;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String keyDecisions;

    @Column(columnDefinition = "TEXT")
    private String actionItems;

    private String fileName;

    private String originalFileName;

    private String fileType;

    private Long fileSize;

    private String status;

    private String processingStage;

    private Integer progressPercent;

    @Column(columnDefinition = "TEXT")
    private String processingError;


    public Meeting()
    {
    }


    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }


    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }


    public String getTranscript()
    {
        return transcript;
    }

    public void setTranscript(String transcript)
    {
        this.transcript = transcript;
    }


    public String getTranscriptSegments() { return transcriptSegments; }

    public void setTranscriptSegments(String transcriptSegments) { this.transcriptSegments = transcriptSegments; }

    public Double getTranscriptionSeconds() { return transcriptionSeconds; }

    public void setTranscriptionSeconds(Double transcriptionSeconds) { this.transcriptionSeconds = transcriptionSeconds; }

    public Double getAnalysisSeconds() { return analysisSeconds; }

    public void setAnalysisSeconds(Double analysisSeconds) { this.analysisSeconds = analysisSeconds; }

    public String getSummary()
    {
        return summary;
    }

    public void setSummary(String summary)
    {
        this.summary = summary;
    }


    public String getKeyDecisions()
    {
        return keyDecisions;
    }

    public void setKeyDecisions(String keyDecisions)
    {
        this.keyDecisions = keyDecisions;
    }


    public String getActionItems()
    {
        return actionItems;
    }

    public void setActionItems(String actionItems)
    {
        this.actionItems = actionItems;
    }


    public String getFileName()
    {
        return fileName;
    }

    public void setFileName(String fileName)
    {
        this.fileName = fileName;
    }


    public String getOriginalFileName()
    {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName)
    {
        this.originalFileName = originalFileName;
    }


    public String getFileType()
    {
        return fileType;
    }

    public void setFileType(String fileType)
    {
        this.fileType = fileType;
    }


    public Long getFileSize()
    {
        return fileSize;
    }

    public void setFileSize(Long fileSize)
    {
        this.fileSize = fileSize;
    }


    public String getProcessingStage() { return processingStage; }

    public void setProcessingStage(String processingStage) { this.processingStage = processingStage; }

    public Integer getProgressPercent() { return progressPercent; }

    public void setProgressPercent(Integer progressPercent) { this.progressPercent = progressPercent; }

    public String getProcessingError() { return processingError; }

    public void setProcessingError(String processingError) { this.processingError = processingError; }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }
}