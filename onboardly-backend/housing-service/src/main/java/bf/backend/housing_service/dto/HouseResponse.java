package bf.backend.housing_service.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class HouseResponse {

    private Long id;

    private String address;

    private Integer maxOccupant;

    private Long landlordId;

    private String landlordName;

    private String landlordEmail;

    private String landlordPhone;

    private LocalDateTime createDate;

    private LocalDateTime lastModificationDate;
}