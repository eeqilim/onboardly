package bf.backend.employee_service.dto.response;

public record HouseOccupancyResponse(
        Long houseId,
        String address,
        Integer maxOccupant,
        int currentOccupants,
        int availableSpots,
        Long landlordId,
        String landlordName,
        String landlordEmail,
        String landlordPhone
) {}
