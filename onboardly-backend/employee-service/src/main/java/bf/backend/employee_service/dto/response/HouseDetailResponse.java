package bf.backend.employee_service.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class HouseDetailResponse {
    private Long houseId;
    private String address;
    private List<HouseResidentResponse> residents;
}