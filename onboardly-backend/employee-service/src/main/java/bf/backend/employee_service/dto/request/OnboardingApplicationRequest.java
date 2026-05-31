package bf.backend.employee_service.dto.request;

import bf.backend.employee_service.entity.CitizenshipStatus;
import bf.backend.employee_service.entity.Gender;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;

public record OnboardingApplicationRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        String middleName,
        String preferredName,
        @NotBlank @Email String email,
        @Pattern(regexp = "\\+?\\d{10,15}") String cellPhone,
        @Pattern(regexp = "\\+?\\d{10,15}") String alternatePhone,
        Gender gender,
        @Pattern(regexp = "\\d{3}-\\d{2}-\\d{4}") String ssn,
        @Past LocalDate dateOfBirth,
        @NotNull CitizenshipStatus citizenshipStatus,
        String driverLicense,
        LocalDate driverLicenseExpiration,
        @NotNull @Valid AddressRequest address,
        @Valid ContactRequest referenceContact,
        @NotEmpty List<@Valid ContactRequest> emergencyContacts,
        @Valid VisaInfoRequest visaInfo
) {}
