package bf.backend.employee_service.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Address {

    private String id;
    private AddressType type;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String zipCode;
}
