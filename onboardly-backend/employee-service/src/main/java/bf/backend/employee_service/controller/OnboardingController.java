package bf.backend.employee_service.controller;

import bf.backend.employee_service.dto.request.AddCommentRequest;
import bf.backend.employee_service.dto.request.OnboardingApplicationRequest;
import bf.backend.employee_service.dto.request.ReviewApplicationRequest;
import bf.backend.employee_service.dto.response.ApplicationCommentResponse;
import bf.backend.employee_service.dto.response.HrOnboardingProfileResponse;
import bf.backend.employee_service.dto.response.OnboardingApplicationResponse;
import bf.backend.employee_service.service.OnboardingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/employee/onboarding")
@RequiredArgsConstructor
@Tag(name = "Onboarding", description = "Employee onboarding lifecycle — start, submit, and HR review")
@SecurityRequirement(name = "BearerAuth")
public class OnboardingController {

    private final OnboardingService onboardingService;

    @GetMapping("/hr/{employeeId}")
    @PreAuthorize("hasRole('HR')")
    @Operation(summary = "Get full onboarding profile for an employee — HR only")
    @ApiResponse(responseCode = "200", description = "Employee onboarding profile returned")
    @ApiResponse(responseCode = "403", description = "HR role required")
    @ApiResponse(responseCode = "404", description = "Employee not found")
    public ResponseEntity<HrOnboardingProfileResponse> getApplicationByEmployeeId(
            @PathVariable String employeeId) {
        return ResponseEntity.ok(onboardingService.getHrOnboardingProfile(employeeId));
    }

    @GetMapping("/me")
    @Operation(summary = "Get the current user's onboarding application")
    @ApiResponse(responseCode = "200", description = "Application returned")
    @ApiResponse(responseCode = "404", description = "No application found — call POST /start first")
    public ResponseEntity<OnboardingApplicationResponse> getMyApplication() {
        return ResponseEntity.ok(onboardingService.getMyApplication());
    }

    @PostMapping("/start")
    @Operation(summary = "Initialise employee record and NOT_STARTED application after registration (idempotent)")
    @ApiResponse(responseCode = "200", description = "Application initialised or existing application returned")
    public ResponseEntity<OnboardingApplicationResponse> startApplication() {
        return ResponseEntity.ok(onboardingService.startApplication());
    }

    @PostMapping("/submit")
    @Operation(summary = "Submit onboarding form — populates employee profile, validates documents, sets status PENDING")
    @ApiResponse(responseCode = "200", description = "Application submitted and awaiting HR review")
    @ApiResponse(responseCode = "400", description = "Validation error or missing required documents")
    @ApiResponse(responseCode = "404", description = "No employee record found — call POST /start first")
    public ResponseEntity<OnboardingApplicationResponse> submitApplication(
            @RequestBody @Valid OnboardingApplicationRequest req) {
        return ResponseEntity.ok(onboardingService.submitApplication(req));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('HR')")
    @Operation(summary = "List all PENDING onboarding applications — HR only")
    @ApiResponse(responseCode = "200", description = "List of pending applications")
    @ApiResponse(responseCode = "403", description = "HR role required")
    public ResponseEntity<List<OnboardingApplicationResponse>> listPendingApplications() {
        return ResponseEntity.ok(onboardingService.listPendingApplications());
    }

    @PostMapping("/{applicationId}/comment")
    @PreAuthorize("hasRole('HR')")
    @Operation(summary = "Add a comment to an onboarding application — HR only")
    @ApiResponse(responseCode = "200", description = "Comment added")
    @ApiResponse(responseCode = "403", description = "HR role required")
    @ApiResponse(responseCode = "404", description = "Application not found")
    public ResponseEntity<ApplicationCommentResponse> addComment(
            @PathVariable String applicationId,
            @RequestBody @Valid AddCommentRequest req) {
        return ResponseEntity.ok(onboardingService.addComment(applicationId, req));
    }

    @PostMapping("/{applicationId}/review")
    @PreAuthorize("hasRole('HR')")
    @Operation(summary = "Approve or reject a PENDING application — HR only")
    @ApiResponse(responseCode = "200", description = "Application reviewed")
    @ApiResponse(responseCode = "400", description = "Application is not in PENDING status, or invalid review status")
    @ApiResponse(responseCode = "403", description = "HR role required")
    @ApiResponse(responseCode = "404", description = "Application not found")
    public ResponseEntity<OnboardingApplicationResponse> reviewApplication(
            @PathVariable String applicationId,
            @RequestBody @Valid ReviewApplicationRequest req) {
        return ResponseEntity.ok(onboardingService.reviewApplication(applicationId, req));
    }
}
