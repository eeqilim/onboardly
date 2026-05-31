package bf.backend.housing_service.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class FacilityReportDetailResponse {

    private Long id;

    private Long facilityReportId;

    private Long employeeId;

    private String comment;

    private LocalDateTime createDate;

    private LocalDateTime lastModificationDate;

    private String commenterName;
}