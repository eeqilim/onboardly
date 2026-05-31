package bf.backend.employee_service.dto.request;

import bf.backend.employee_service.entity.VisaType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record VisaInfoRequest(
        @NotNull VisaType visaType,
        String visaTypeOther,
        LocalDate startDate,
        LocalDate endDate
) {}
