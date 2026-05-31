package bf.backend.employee_service.controller;

import bf.backend.employee_service.dto.response.ApplicationTrackingItem;
import bf.backend.employee_service.dto.response.EmployeeSummaryResponse;
import bf.backend.employee_service.dto.response.HouseOccupancyResponse;
import bf.backend.employee_service.dto.response.HrHouseSummaryResponse;
import bf.backend.employee_service.dto.response.PageResponse;
import bf.backend.employee_service.service.HrEmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/employee/hr")
@RequiredArgsConstructor
@PreAuthorize("hasRole('HR')")
@Tag(name = "HR", description = "HR-only endpoints — employee roster, search, and application tracking")
@SecurityRequirement(name = "BearerAuth")
public class HrController {

    private final HrEmployeeService hrEmployeeService;

    @GetMapping("/employees")
    @Operation(summary = "Paginated employee list ordered by ID (10 per page)")
    @ApiResponse(responseCode = "200", description = "Page of employee summaries returned")
    @ApiResponse(responseCode = "403", description = "HR role required")
    public ResponseEntity<PageResponse<EmployeeSummaryResponse>> listEmployees(
            @RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok(hrEmployeeService.listEmployees(page));
    }

    @GetMapping("/employees/search")
    @Operation(summary = "Search employees by first name, last name, or preferred name (case-insensitive)")
    @ApiResponse(responseCode = "200", description = "Matching employee summaries returned")
    @ApiResponse(responseCode = "403", description = "HR role required")
    public ResponseEntity<List<EmployeeSummaryResponse>> searchEmployees(
            @RequestParam("q") String query) {
        return ResponseEntity.ok(hrEmployeeService.searchEmployees(query));
    }

    @GetMapping("/house/{houseId}/residents")
    @Operation(summary = "Get all employees assigned to a house — HR only")
    @ApiResponse(responseCode = "200", description = "Resident list returned")
    @ApiResponse(responseCode = "403", description = "HR role required")
    public ResponseEntity<List<EmployeeSummaryResponse>> getResidentsByHouseId(
            @PathVariable Long houseId) {
        return ResponseEntity.ok(hrEmployeeService.getResidentsByHouseId(houseId));
    }

    @GetMapping("/tracking")
    @Operation(summary = "Combined application tracking — PENDING onboarding and STEM OPT EAD uploads awaiting review")
    @ApiResponse(responseCode = "200", description = "Tracking items returned")
    @ApiResponse(responseCode = "403", description = "HR role required")
    public ResponseEntity<List<ApplicationTrackingItem>> getApplicationTrackingItems() {
        return ResponseEntity.ok(hrEmployeeService.getApplicationTrackingItems());
    }

    @PutMapping("/employees/{employeeId}/house/{houseId}")
    @Operation(summary = "Assign or reassign an employee to a house — validates against maxOccupant")
    @ApiResponse(responseCode = "200", description = "Employee housing assignment updated")
    @ApiResponse(responseCode = "400", description = "House is at full capacity")
    @ApiResponse(responseCode = "403", description = "HR role required")
    @ApiResponse(responseCode = "404", description = "Employee or house not found")
    public ResponseEntity<EmployeeSummaryResponse> assignHouse(
            @PathVariable String employeeId,
            @PathVariable Long houseId) {
        return ResponseEntity.ok(hrEmployeeService.assignHouse(employeeId, houseId));
    }

    @DeleteMapping("/employees/{employeeId}/house")
    @Operation(summary = "Remove an employee from their current house")
    @ApiResponse(responseCode = "200", description = "Employee removed from house")
    @ApiResponse(responseCode = "403", description = "HR role required")
    @ApiResponse(responseCode = "404", description = "Employee not found")
    public ResponseEntity<EmployeeSummaryResponse> removeHouse(
            @PathVariable String employeeId) {
        return ResponseEntity.ok(hrEmployeeService.removeHouse(employeeId));
    }

    @GetMapping("/housing/available")
    @Operation(summary = "List all houses that have at least one available spot")
    @ApiResponse(responseCode = "200", description = "Available houses returned with current occupancy")
    @ApiResponse(responseCode = "403", description = "HR role required")
    public ResponseEntity<List<HouseOccupancyResponse>> getAvailableHouses() {
        return ResponseEntity.ok(hrEmployeeService.getAvailableHouses());
    }

    @GetMapping("/housing/summary")
    @Operation(summary = "List all houses with current occupants and resident details")
    @ApiResponse(responseCode = "200", description = "House summaries returned with resident lists")
    @ApiResponse(responseCode = "403", description = "HR role required")
    public ResponseEntity<List<HrHouseSummaryResponse>> getHousingSummary() {
        return ResponseEntity.ok(hrEmployeeService.getHousingSummary());
    }
}
