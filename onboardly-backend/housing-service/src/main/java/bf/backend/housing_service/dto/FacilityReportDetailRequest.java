package bf.backend.housing_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FacilityReportDetailRequest {

    @NotNull(message = "Facility report id is required")
    @Positive(message = "Facility report id must be greater than 0")
    private Long facilityReportId;

    @NotNull(message = "Employee id is required")
    @Positive(message = "Employee id must be greater than 0")
    private Long employeeId;

    @NotBlank(message = "Comment cannot be empty")
    private String comment;
}