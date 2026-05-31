package bf.backend.employee_service.controller;

import bf.backend.employee_service.dto.response.EmployeeNameResponse;
import bf.backend.employee_service.exception.ResourceNotFoundException;
import bf.backend.employee_service.repository.EmployeeRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/employee/users")
@RequiredArgsConstructor
@Tag(name = "Employee Lookup", description = "Internal lookup endpoints used by other services via Feign")
@SecurityRequirement(name = "BearerAuth")
public class EmployeeLookupController {

    private final EmployeeRepository employeeRepository;

    @GetMapping("/{userId}/name")
    @Operation(summary = "Get an employee's name by auth-server userId — used by housing-service Feign client")
    @ApiResponse(responseCode = "200", description = "Employee name returned")
    @ApiResponse(responseCode = "404", description = "No employee found for this userId")
    public ResponseEntity<EmployeeNameResponse> getEmployeeNameByUserId(@PathVariable Long userId) {
        return employeeRepository.findByUserId(userId)
                .map(e -> ResponseEntity.ok(new EmployeeNameResponse(
                        e.getId(),
                        e.getUserId(),
                        e.getFirstName(),
                        e.getLastName(),
                        e.getPreferredName()
                )))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No employee found for userId: " + userId));
    }
}
