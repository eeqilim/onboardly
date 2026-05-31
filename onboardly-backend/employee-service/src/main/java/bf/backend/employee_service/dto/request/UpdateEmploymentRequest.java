package bf.backend.employee_service.dto.request;

import java.time.LocalDate;

public record UpdateEmploymentRequest(
        LocalDate employmentStartDate,
        LocalDate employmentEndDate,
        String driverLicense,
        LocalDate driverLicenseExpiration,
        Long houseId
) {}
