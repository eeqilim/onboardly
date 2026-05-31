package bf.backend.employee_service.dto.request;

import bf.backend.employee_service.entity.ApplicationStatus;
import jakarta.validation.constraints.NotNull;

public record ReviewApplicationRequest(
        @NotNull ApplicationStatus status,
        String hrFeedback
) {}
