package bf.backend.housing_service.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class FacilityResponse {

    private Long id;

    private Long houseId;

    private String type;

    private String description;

    private Integer quantity;
}