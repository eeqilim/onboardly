package bf.backend.employee_service.dto.response;

import java.util.List;

public record HrHouseSummaryResponse(
        Long houseId,
        String address,
        Integer maxOccupant,
        int currentOccupants,
        int availableSpots,
        Long landlordId,
        String landlordName,
        String landlordEmail,
        String landlordPhone,
        List<HrHouseResidentResponse> residents
) {}
