package bf.backend.employee_service.dto.response;

import bf.backend.employee_service.entity.VisaType;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record VisaStatusResponse(
        String id,
        VisaType visaType,
        String visaTypeOther,
        Boolean activeFlag,
        LocalDate startDate,
        LocalDate endDate,
        LocalDateTime lastModificationDate
) {}
