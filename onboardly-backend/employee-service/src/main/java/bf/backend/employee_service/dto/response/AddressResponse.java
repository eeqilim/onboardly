package bf.backend.employee_service.dto.response;

import bf.backend.employee_service.entity.AddressType;

public record AddressResponse(
        String id,
        AddressType type,
        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String zipCode
) {}
