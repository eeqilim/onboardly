package bf.backend.housing_service.controller;

import bf.backend.housing_service.dto.FacilityReportDetailRequest;
import bf.backend.housing_service.dto.FacilityReportDetailResponse;
import bf.backend.housing_service.service.FacilityReportDetailService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/housing/report-details")
public class FacilityReportDetailController {

    private final FacilityReportDetailService detailService;

    public FacilityReportDetailController(FacilityReportDetailService detailService) {
        this.detailService = detailService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('HR','EMPLOYEE')")
    public FacilityReportDetailResponse createDetail(@Valid @RequestBody FacilityReportDetailRequest request) {
        return detailService.createDetail(request);
    }

    @GetMapping
    @PreAuthorize("hasRole('HR')")
    public List<FacilityReportDetailResponse> getAllDetails() {
        return detailService.getAllDetails();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR','EMPLOYEE')")
    public FacilityReportDetailResponse getDetailById(@PathVariable Long id) {
        return detailService.getDetailById(id);
    }

    @GetMapping("/report/{reportId}")
    @PreAuthorize("hasAnyRole('HR','EMPLOYEE')")
    public List<FacilityReportDetailResponse> getDetailsByReportId(@PathVariable Long reportId) {
        return detailService.getDetailsByReportId(reportId);
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('HR','EMPLOYEE')")
    public List<FacilityReportDetailResponse> getDetailsByEmployeeId(@PathVariable Long employeeId) {
        return detailService.getDetailsByEmployeeId(employeeId);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR','EMPLOYEE')")
    public FacilityReportDetailResponse updateDetail(@PathVariable Long id,
                                                     @Valid @RequestBody FacilityReportDetailRequest request) {
        return detailService.updateDetail(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('HR')")
    public String deleteDetail(@PathVariable Long id) {
        detailService.deleteDetail(id);
        return "Facility report detail deleted successfully";
    }
}
