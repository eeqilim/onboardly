package bf.backend.employee_service.mapper;

import bf.backend.employee_service.dto.request.AddressRequest;
import bf.backend.employee_service.dto.response.AddressResponse;
import bf.backend.employee_service.entity.Address;

public final class AddressMapper {

    private AddressMapper() {}

    public static AddressResponse toResponse(Address a) {
        return new AddressResponse(
                a.getId(),
                a.getType(),
                a.getAddressLine1(),
                a.getAddressLine2(),
                a.getCity(),
                a.getState(),
                a.getZipCode()
        );
    }

    public static Address toEntity(AddressRequest r) {
        Address a = new Address();
        a.setType(r.type());
        a.setAddressLine1(r.addressLine1());
        a.setAddressLine2(r.addressLine2());
        a.setCity(r.city());
        a.setState(r.state());
        a.setZipCode(r.zipCode());
        return a;
    }
}
