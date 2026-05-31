package bf.backend.employee_service.dto.request;

import jakarta.validation.constraints.NotBlank;

public record HrCommentRequest(@NotBlank String comment) {}
