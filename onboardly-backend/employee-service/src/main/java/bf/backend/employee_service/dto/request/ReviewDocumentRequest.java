package bf.backend.employee_service.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ReviewDocumentRequest(
        @NotBlank String comment
) {}
