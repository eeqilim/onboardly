package bf.backend.employee_service.dto.response;

public record ApplicationServiceDataResponse<T>(
        String message,
        T data
) {}
