package bf.backend.housing_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HouseRequest {

    @NotBlank(message = "Address cannot be empty")
    private String address;

    @NotNull(message = "Max occupant is required")
    @Positive(message = "Max occupant must be greater than 0")
    private Integer maxOccupant;

    @NotNull(message = "Landlord id is required")
    @Positive(message = "Landlord id must be greater than 0")
    private Long landlordId;
}