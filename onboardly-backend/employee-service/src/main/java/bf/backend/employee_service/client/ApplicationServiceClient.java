package bf.backend.employee_service.client;

import bf.backend.employee_service.config.FeignConfig;
import bf.backend.employee_service.dto.request.ApplicationDocumentMetadataRequest;
import bf.backend.employee_service.dto.request.ApplicationWorkflowRequest;
import bf.backend.employee_service.dto.response.ApplicationServiceDataResponse;
import bf.backend.employee_service.dto.response.ApplicationWorkflowResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import jakarta.validation.Valid;
import java.util.List;

@FeignClient(name = "application-service", configuration = FeignConfig.class)
public interface ApplicationServiceClient {
    @PostMapping("/application")
    ApplicationServiceDataResponse<ApplicationWorkflowResponse> createApplication(
            @Valid @RequestBody ApplicationWorkflowRequest request);

    @GetMapping("/application/employee/{employeeId}/application-type/{applicationType}")
    ApplicationServiceDataResponse<List<ApplicationWorkflowResponse>> getApplicationsByEmployeeIdAndType(
            @PathVariable Long employeeId,
            @PathVariable String applicationType);

    @PostMapping("/application/documents")
    void createDocumentMetadata(@Valid @RequestBody ApplicationDocumentMetadataRequest request);
}
