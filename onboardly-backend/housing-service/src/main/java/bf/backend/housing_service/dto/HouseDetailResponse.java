package bf.backend.housing_service.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class HouseDetailResponse {

    private Long id;

    private String address;

    private Integer maxOccupant;

    private Long landlordId;

    private String landlordName;

    private String landlordEmail;

    private String landlordPhone;

    private List<FacilityResponse> facilities;

    private List<FacilityReportResponse> recentReports;

    private LocalDateTime createDate;

    private LocalDateTime lastModificationDate;
}
