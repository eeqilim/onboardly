package bf.backend.employee_service.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Contact {

    private String id;
    private ContactType type;
    private String firstName;
    private String lastName;
    private String middleName;
    private String cellPhone;
    private String alternatePhone;
    private String email;
    private String relationship;
    private String address;
}
