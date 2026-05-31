package bf.backend.employee_service.dto.response;

import bf.backend.employee_service.entity.StemOptStep;
import bf.backend.employee_service.entity.VisaType;

import java.time.LocalDate;

public record VisaSummaryResponse(
        String visaStatusId,
        String employeeId,
        String firstName,
        String lastName,
        VisaType workAuth,
        String visaTypeOther,
        LocalDate expirationDate,
        long daysLeft,
        boolean hasActiveStemOptApp,
        StemOptStep currentStep
) {}
