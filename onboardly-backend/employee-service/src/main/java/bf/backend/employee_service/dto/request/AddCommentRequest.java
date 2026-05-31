package bf.backend.employee_service.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AddCommentRequest(
        @NotBlank String content
) {}
