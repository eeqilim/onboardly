package bf.backend.employee_service.dto.response;

import bf.backend.employee_service.entity.DocumentType;

import java.time.LocalDateTime;

public record DocumentResponse(
        String id,
        DocumentType documentType,
        String s3Key,
        String title,
        String comment,
        String applicationType,
        LocalDateTime createdAt,
        LocalDateTime lastModificationDate
) {}
