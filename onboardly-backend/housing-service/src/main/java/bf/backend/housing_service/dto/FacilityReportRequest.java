package bf.backend.housing_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FacilityReportRequest {

    @NotNull(message = "Facility id is required")
    @Positive(message = "Facility id must be greater than 0")
    private Long facilityId;

    @NotNull(message = "Employee id is required")
    @Positive(message = "Employee id must be greater than 0")
    private Long employeeId;

    @NotBlank(message = "Title cannot be empty")
    private String title;

    @NotBlank(message = "Description cannot be empty")
    private String description;
}