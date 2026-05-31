package bf.backend.employee_service.dto.request;

import bf.backend.employee_service.entity.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public record UpdatePersonalRequest(
        @NotBlank @Email String email,
        @Email String personalEmail,
        @Pattern(regexp = "\\+?\\d{10,15}") String cellPhone,
        @Pattern(regexp = "\\+?\\d{10,15}") String alternatePhone,
        @Pattern(regexp = "\\+?\\d{10,15}") String workPhone,
        Gender gender,
        LocalDate dateOfBirth
) {}
