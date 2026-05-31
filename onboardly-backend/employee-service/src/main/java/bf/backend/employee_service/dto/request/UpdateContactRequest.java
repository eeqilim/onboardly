package bf.backend.employee_service.dto.request;

import bf.backend.employee_service.entity.ContactType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record UpdateContactRequest(
        @NotNull ContactType type,
        @NotBlank String firstName,
        @NotBlank String lastName,
        String middleName,
        @Pattern(regexp = "\\+?\\d{10,15}") String cellPhone,
        @Pattern(regexp = "\\+?\\d{10,15}") String alternatePhone,
        @Email String email,
        String relationship,
        String address
) {}
