package bf.backend.employee_service.dto.response;

import java.time.LocalDateTime;

public record ApplicationTrackingItem(
        String referenceId,
        String employeeId,
        String employeeName,
        String applicationType,
        String status,
        LocalDateTime lastModificationDate
) {}
