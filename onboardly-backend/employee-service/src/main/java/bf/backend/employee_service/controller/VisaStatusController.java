package bf.backend.employee_service.controller;

import bf.backend.employee_service.dto.request.ReviewApplicationRequest;
import bf.backend.employee_service.dto.response.StemOptProgressResponse;
import bf.backend.employee_service.dto.response.VisaSummaryResponse;
import bf.backend.employee_service.dto.response.VisaStatusResponse;
import bf.backend.employee_service.entity.StemOptStep;
import bf.backend.employee_service.service.VisaStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/employee/visa")
@RequiredArgsConstructor
@Tag(name = "Visa Status", description = "Visa status management and OPT/STEM OPT workflow")
@SecurityRequirement(name = "BearerAuth")
public class VisaStatusController {

    private final VisaStatusService visaStatusService;

    // ── employee endpoints ────────────────────────────────────────────────────

    @GetMapping("/me/active")
    @Operation(summary = "Get the current user's active visa")
    @ApiResponse(responseCode = "200", description = "Active visa returned")
    @ApiResponse(responseCode = "404", description = "No active visa found")
    public ResponseEntity<VisaStatusResponse> getMyActiveVisa() {
        return ResponseEntity.ok(visaStatusService.getMyActiveVisa());
    }

    @GetMapping("/me/history")
    @Operation(summary = "Get the current user's full visa history (all records, newest first)")
    @ApiResponse(responseCode = "200", description = "History returned")
    public ResponseEntity<List<VisaStatusResponse>> getMyVisaHistory() {
        return ResponseEntity.ok(visaStatusService.getMyVisaHistory());
    }

    @GetMapping("/me/stem-opt/progress")
    @Operation(summary = "Get the current user's STEM OPT document upload progress")
    @ApiResponse(responseCode = "200", description = "Progress returned")
    @ApiResponse(responseCode = "404", description = "Employee record not found")
    public ResponseEntity<StemOptProgressResponse> getMyStempOptProgress() {
        return ResponseEntity.ok(visaStatusService.getMyStemOptProgress());
    }

    @PostMapping(value = "/me/stem-opt/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a STEM OPT document for the current step (sequential — previous step must exist)")
    @ApiResponse(responseCode = "200", description = "Document uploaded and visa status updated")
    @ApiResponse(responseCode = "400", description = "Previous step not yet uploaded, or visa type not F1_OPT_STEM")
    @ApiResponse(responseCode = "404", description = "No active visa found")
    public ResponseEntity<VisaStatusResponse> uploadStemOptDocument(
            @RequestParam StemOptStep step,
            @RequestParam MultipartFile file,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate eadStartDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate eadEndDate) {
        return ResponseEntity.ok(
                visaStatusService.uploadStemOptDocument(step, file, eadStartDate, eadEndDate));
    }

    // ── HR endpoints ──────────────────────────────────────────────────────────

    @GetMapping("/hr/all")
    @PreAuthorize("hasRole('HR')")
    @Operation(summary = "List all employees with active visa statuses — HR only")
    @ApiResponse(responseCode = "200", description = "Summary list returned")
    @ApiResponse(responseCode = "403", description = "HR role required")
    public ResponseEntity<List<VisaSummaryResponse>> listAllVisaStatuses() {
        return ResponseEntity.ok(visaStatusService.listAllVisaStatuses());
    }

    @PostMapping("/hr/{visaStatusId}/review")
    @PreAuthorize("hasRole('HR')")
    @Operation(summary = "Approve or reject a STEM OPT EAD upload — HR only")
    @ApiResponse(responseCode = "200", description = "Review recorded; REJECTED deactivates the visa record")
    @ApiResponse(responseCode = "400", description = "Invalid review status (must be APPROVED or REJECTED)")
    @ApiResponse(responseCode = "403", description = "HR role required")
    @ApiResponse(responseCode = "404", description = "Visa status not found")
    public ResponseEntity<VisaStatusResponse> reviewStemOptApplication(
            @PathVariable String visaStatusId,
            @RequestBody @Valid ReviewApplicationRequest req) {
        return ResponseEntity.ok(visaStatusService.reviewStemOptApplication(visaStatusId, req));
    }
}
