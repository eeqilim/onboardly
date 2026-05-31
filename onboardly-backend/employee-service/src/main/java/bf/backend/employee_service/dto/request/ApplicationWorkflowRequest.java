package bf.backend.employee_service.dto.request;

import lombok.Builder;

@Builder
public record ApplicationWorkflowRequest(
        Long employeeId,
        String applicationType
) {}
