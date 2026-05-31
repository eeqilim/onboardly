package bf.backend.employee_service.dto.response;

import bf.backend.employee_service.entity.CitizenshipStatus;
import bf.backend.employee_service.entity.Gender;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record EmployeeProfileResponse(
        String id,
        Long userId,
        String firstName,
        String lastName,
        String middleName,
        String preferredName,
        String email,
        String personalEmail,
        String cellPhone,
        String alternatePhone,
        String workPhone,
        Gender gender,
        String ssn,
        LocalDate dateOfBirth,
        String avatarUrl,
        CitizenshipStatus citizenshipStatus,
        String driverLicense,
        LocalDate driverLicenseExpiration,
        Long houseId,
        LocalDate employmentStartDate,
        LocalDate employmentEndDate,
        LocalDateTime createdAt,
        List<AddressResponse> addresses,
        List<ContactResponse> contacts,
        List<VisaStatusResponse> visaStatuses,
        List<DocumentResponse> documents,
        List<OnboardingApplicationResponse> onboardingApplications
) {}
