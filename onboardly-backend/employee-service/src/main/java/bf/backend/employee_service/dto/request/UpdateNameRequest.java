package bf.backend.employee_service.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateNameRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        String middleName,
        String preferredName
) {}
