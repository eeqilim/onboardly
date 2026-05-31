package bf.backend.employee_service.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HouseResidentResponse {
    private String employeeId;
    private String firstName;
    private String lastName;
    private String preferredName;
    private String cellPhone;
}