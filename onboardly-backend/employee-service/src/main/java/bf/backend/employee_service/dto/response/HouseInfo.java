package bf.backend.employee_service.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HouseInfo(
        Long id,
        String address,
        Integer maxOccupant,
        Long landlordId,
        String landlordName,
        String landlordEmail,
        String landlordPhone
) {}
