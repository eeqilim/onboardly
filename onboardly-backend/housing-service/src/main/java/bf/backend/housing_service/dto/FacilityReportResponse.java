package bf.backend.housing_service.dto;

import bf.backend.housing_service.entity.ReportStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class FacilityReportResponse {

    private Long id;

    private Long facilityId;

    private Long employeeId;

    private String title;

    private String description;

    private ReportStatus status;

    private LocalDateTime createDate;

    private LocalDateTime lastModificationDate;

    private String createdByName;
}