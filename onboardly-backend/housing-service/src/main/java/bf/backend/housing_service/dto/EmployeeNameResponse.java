package bf.backend.housing_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmployeeNameResponse {
    private String employeeId;
    private Long userId;
    private String firstName;
    private String lastName;
    private String preferredName;
}