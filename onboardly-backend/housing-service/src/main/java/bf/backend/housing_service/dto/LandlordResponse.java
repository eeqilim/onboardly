package bf.backend.housing_service.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class LandlordResponse {

    private Long id;

    private String firstName;

    private String lastName;

    private String email;

    private String cellPhone;

    private LocalDateTime createDate;

    private LocalDateTime lastModificationDate;
}