package bf.backend.housing_service.controller;

import bf.backend.housing_service.dto.FacilityReportRequest;
import bf.backend.housing_service.dto.FacilityReportResponse;
import bf.backend.housing_service.dto.FacilityReportWithDetailsResponse;
import bf.backend.housing_service.entity.ReportStatus;
import bf.backend.housing_service.service.FacilityReportService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/housing/reports")
public class FacilityReportController {

    private final FacilityReportService facilityReportService;

    public FacilityReportController(FacilityReportService facilityReportService) {
        this.facilityReportService = facilityReportService;
    }

    @PostMapping
    @PreAuthorize("hasRole('EMPLOYEE')")
    public FacilityReportResponse createFacilityReport(@Valid @RequestBody FacilityReportRequest request) {
        return facilityReportService.createFacilityReport(request);
    }

    @GetMapping
    @PreAuthorize("hasRole('HR')")
    public List<FacilityReportResponse> getAllFacilityReports() {
        return facilityReportService.getAllFacilityReports();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR','EMPLOYEE')")
    public FacilityReportResponse getFacilityReportById(@PathVariable Long id) {
        return facilityReportService.getFacilityReportById(id);
    }

    @GetMapping("/{id}/details")
    @PreAuthorize("hasAnyRole('HR','EMPLOYEE')")
    public FacilityReportWithDetailsResponse getFacilityReportWithDetails(@PathVariable Long id) {
        return facilityReportService.getFacilityReportWithDetails(id);
    }

    @GetMapping("/facility/{facilityId}")
    @PreAuthorize("hasRole('HR')")
    public List<FacilityReportResponse> getFacilityReportsByFacilityId(@PathVariable Long facilityId) {
        return facilityReportService.getFacilityReportsByFacilityId(facilityId);
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('HR','EMPLOYEE')")
    public List<FacilityReportResponse> getFacilityReportsByEmployeeId(@PathVariable Long employeeId) {
        return facilityReportService.getFacilityReportsByEmployeeId(employeeId);
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('HR')")
    public List<FacilityReportResponse> getFacilityReportsByStatus(@PathVariable ReportStatus status) {
        return facilityReportService.getFacilityReportsByStatus(status);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR','EMPLOYEE')")
    public FacilityReportResponse updateFacilityReport(@PathVariable Long id,
                                                       @Valid @RequestBody FacilityReportRequest request) {
        return facilityReportService.updateFacilityReport(id, request);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('HR')")
    public FacilityReportResponse updateFacilityReportStatus(@PathVariable Long id,
                                                             @RequestParam ReportStatus status) {
        return facilityReportService.updateFacilityReportStatus(id, status);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('HR')")
    public String deleteFacilityReport(@PathVariable Long id) {
        facilityReportService.deleteFacilityReport(id);
        return "Facility report deleted successfully";
    }
}
