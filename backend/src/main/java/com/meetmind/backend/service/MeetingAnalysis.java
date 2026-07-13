package com.meetmind.backend.service;

import java.util.List;

public record MeetingAnalysis(
        String summary,
        List<String> keyDecisions,
        List<ActionItem> actionItems)
{
    public record ActionItem(
            String task,
            String owner,
            String dueDate,
            String priority,
            String evidence)
    {
    }
}