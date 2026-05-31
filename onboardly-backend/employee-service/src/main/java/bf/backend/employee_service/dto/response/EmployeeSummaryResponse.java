package bf.backend.employee_service.dto.response;

import java.time.LocalDate;

public record EmployeeSummaryResponse(
        String id,
        Long userId,
        String firstName,
        String lastName,
        String preferredName,
        String email,
        String ssn,
        LocalDate employmentStartDate,
        VisaStatusResponse activeVisa
) {}
