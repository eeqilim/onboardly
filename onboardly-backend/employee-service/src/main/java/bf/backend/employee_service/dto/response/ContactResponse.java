package bf.backend.employee_service.dto.response;

import bf.backend.employee_service.entity.ContactType;

public record ContactResponse(
        String id,
        ContactType type,
        String firstName,
        String lastName,
        String middleName,
        String cellPhone,
        String alternatePhone,
        String email,
        String relationship,
        String address
) {}
