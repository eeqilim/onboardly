package bf.backend.employee_service.dto.response;

public record HrHouseResidentResponse(
        String employeeId,
        Long userId,
        String firstName,
        String lastName,
        String preferredName,
        String email,
        String cellPhone
) {}
