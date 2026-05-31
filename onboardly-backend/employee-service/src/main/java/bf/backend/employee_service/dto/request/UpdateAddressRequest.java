package bf.backend.employee_service.dto.request;

import bf.backend.employee_service.entity.AddressType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateAddressRequest(
        @NotNull AddressType type,
        @NotBlank String addressLine1,
        String addressLine2,
        @NotBlank String city,
        @NotBlank String state,
        @NotBlank String zipCode
) {}
