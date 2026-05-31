package bf.backend.employee_service.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ApplicationWorkflowResponse(
        Integer id,
        Long employeeId,
        LocalDateTime createDate,
        LocalDateTime lastModificationDate,
        String status,
        String currentStep,
        String comment,
        String applicationType
) {}
