package bf.backend.employee_service.mapper;

import bf.backend.employee_service.dto.request.ContactRequest;
import bf.backend.employee_service.dto.response.ContactResponse;
import bf.backend.employee_service.entity.Contact;

import java.util.UUID;

public final class ContactMapper {

    private ContactMapper() {}

    public static ContactResponse toResponse(Contact c) {
        return new ContactResponse(
                c.getId(),
                c.getType(),
                c.getFirstName(),
                c.getLastName(),
                c.getMiddleName(),
                c.getCellPhone(),
                c.getAlternatePhone(),
                c.getEmail(),
                c.getRelationship(),
                c.getAddress()
        );
    }

    public static Contact toEntity(ContactRequest r) {
        Contact c = new Contact();
        c.setId(UUID.randomUUID().toString());
        c.setType(r.type());
        c.setFirstName(r.firstName());
        c.setLastName(r.lastName());
        c.setMiddleName(r.middleName());
        c.setCellPhone(r.cellPhone());
        c.setAlternatePhone(r.alternatePhone());
        c.setEmail(r.email());
        c.setRelationship(r.relationship());
        c.setAddress(r.address());
        return c;
    }
}
