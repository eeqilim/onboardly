package bf.backend.employee_service.controller;

import bf.backend.employee_service.dto.response.HouseDetailResponse;
import bf.backend.employee_service.dto.response.HouseResidentResponse;
import bf.backend.employee_service.service.EmployeeHousingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/employee/housing")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('EMPLOYEE', 'HR')")
@Tag(name = "Employee Housing", description = "Employee housing endpoints")
@SecurityRequirement(name = "BearerAuth")
public class EmployeeHousingController {

    private final EmployeeHousingService employeeHousingService;

    @GetMapping("/residents/me")
    @Operation(summary = "Get residents living in the same house as current employee")
    public ResponseEntity<List<HouseResidentResponse>> getMyHouseResidents() {
        return ResponseEntity.ok(employeeHousingService.getMyHouseResidents());
    }

    @GetMapping("/me")
    @Operation(summary = "Get current employee house detail with residents")
    public ResponseEntity<HouseDetailResponse> getMyHouseDetail() {
        return ResponseEntity.ok(employeeHousingService.getMyHouseDetail());
    }
}