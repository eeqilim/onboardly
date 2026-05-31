package bf.backend.employee_service.dto.response;

import bf.backend.employee_service.entity.ApplicationStatus;

import java.time.LocalDateTime;
import java.util.List;

public record OnboardingApplicationResponse(
        String id,
        String employeeId,
        Integer applicationWorkflowId,
        ApplicationStatus status,
        String hrFeedback,
        LocalDateTime submittedAt,
        LocalDateTime reviewedAt,
        Long reviewedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<ApplicationCommentResponse> comments
) {}
