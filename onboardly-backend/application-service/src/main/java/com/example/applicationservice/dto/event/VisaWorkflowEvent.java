package com.example.applicationservice.dto.event;

import java.util.Map;

public record VisaWorkflowEvent(
        Long userId,
        String eventType,
        String documentS3Key,
        Long timestampMillis,
        Map<String, Object> metadata
) {
}
