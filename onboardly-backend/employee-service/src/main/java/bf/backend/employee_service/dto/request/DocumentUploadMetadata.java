package bf.backend.employee_service.dto.request;

import bf.backend.employee_service.entity.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DocumentUploadMetadata(
        Integer applicationId,
        @NotBlank String title,
        String comment,
        @NotNull DocumentType documentType,
        String applicationType
) {}
