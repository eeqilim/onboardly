package bf.backend.employee_service.dto.response;

public record EmployeeNameResponse(
        String employeeId,
        Long userId,
        String firstName,
        String lastName,
        String preferredName
) {}
