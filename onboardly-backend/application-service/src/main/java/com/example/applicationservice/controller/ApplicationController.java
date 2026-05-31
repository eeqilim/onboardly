package com.example.applicationservice.controller;

import com.example.applicationservice.dto.request.ApplicationRequest;
import com.example.applicationservice.dto.request.ApplicationReviewRequest;
import com.example.applicationservice.dto.request.AdvanceWorkflowRequest;
import com.example.applicationservice.dto.response.ApplicationResponse;
import com.example.applicationservice.dto.response.DataResponse;
import com.example.applicationservice.service.ApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/application")
@RequiredArgsConstructor
@Tag(
        name = "Application Workflow",
        description = "Manage onboarding and OPT/STEM application workflow state. ApplicationService owns status and currentStep."
)
public class ApplicationController {

    private final ApplicationService applicationService;

    // Employee: submit a new application
    @Operation(
            summary = "Create a new application workflow",
            description = "Creates an ONBOARDING or OPT_STEM workflow for an employee. New workflows start with status Open and an initial currentStep."
    )
    @PreAuthorize("hasRole('EMPLOYEE')")
    @PostMapping
    public ResponseEntity<DataResponse> createApplication(@Valid @RequestBody ApplicationRequest request) {
        ApplicationResponse response = applicationService.createApplication(request);
        return ResponseEntity.ok(DataResponse.builder()
                .message("Application created successfully")
                .data(response)
                .build());
    }

    // Employee + HR: get application by id
    @Operation(
            summary = "Get application workflow by ID",
            description = "Returns workflow status, currentStep, comment, application type, and timestamps for one application."
    )
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'HR')")
    @GetMapping("/{id}")
    public ResponseEntity<DataResponse> getApplicationById(@PathVariable Integer id) {
        ApplicationResponse response = applicationService.getApplicationById(id);
        return ResponseEntity.ok(DataResponse.builder()
                .message("Application retrieved successfully")
                .data(response)
                .build());
    }

    // Employee: get all applications by employeeId
    @Operation(
            summary = "Get applications for an employee",
            description = "Returns all application workflows associated with the given Auth userId stored in employeeId."
    )
    @PreAuthorize("hasRole('EMPLOYEE')")
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<DataResponse> getApplicationsByEmployeeId(@PathVariable Long employeeId) {
        List<ApplicationResponse> response = applicationService.getApplicationsByEmployeeId(employeeId);
        return ResponseEntity.ok(DataResponse.builder()
                .message("Applications retrieved successfully")
                .data(response)
                .build());
    }

    @Operation(
            summary = "Get application workflows by employee and application type",
            description = "Returns workflow records filtered by Auth userId stored in employeeId and application type, such as ONBOARDING or OPT_STEM."
    )
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'HR')")
    @GetMapping("/employee/{employeeId}/application-type/{applicationType}")
    public ResponseEntity<DataResponse> getApplicationsByEmployeeIdAndType(@PathVariable Long employeeId,
                    @PathVariable String applicationType) {
            List<ApplicationResponse> response = applicationService.getApplicationsByEmployeeIdAndType(employeeId,
                            applicationType);
            return ResponseEntity.ok(DataResponse.builder()
                            .message("Applications retrieved successfully")
                            .data(response)
                            .build());
    }

    // HR: get all applications
    @Operation(
            summary = "Get all application workflows",
            description = "HR endpoint for viewing all onboarding and OPT/STEM application workflows."
    )
    @PreAuthorize("hasRole('HR')")
    @GetMapping
    public ResponseEntity<DataResponse> getAllApplications() {
        List<ApplicationResponse> response = applicationService.getAllApplications();
        return ResponseEntity.ok(DataResponse.builder()
                .message("All applications retrieved successfully")
                .data(response)
                .build());
    }

    // HR: get all applications by status
    @Operation(
            summary = "Get applications by status",
            description = "Filters workflows by overall lifecycle status, such as Open, Rejected, or Completed."
    )
    @PreAuthorize("hasRole('HR')")
    @GetMapping("/status/{status}")
    public ResponseEntity<DataResponse> getApplicationsByStatus(@PathVariable String status) {
        List<ApplicationResponse> response = applicationService.getApplicationsByStatus(status);
        return ResponseEntity.ok(DataResponse.builder()
                .message("Applications retrieved successfully")
                .data(response)
                .build());
    }

    // HR: get all applications by type
    @Operation(
            summary = "Get applications by type",
            description = "Filters workflows by application type, such as ONBOARDING or OPT_STEM."
    )
    @PreAuthorize("hasRole('HR')")
    @GetMapping("/type/{applicationType}")
    public ResponseEntity<DataResponse> getApplicationsByType(@PathVariable String applicationType) {
        List<ApplicationResponse> response = applicationService.getApplicationsByType(applicationType);
        return ResponseEntity.ok(DataResponse.builder()
                .message("Applications retrieved successfully")
                .data(response)
                .build());
    }

    // HR: approve or reject an application
    @Operation(
            summary = "Review an application workflow",
            description = "HR endpoint to approve or reject an Open workflow. Accepted status values are Completed and Rejected."
    )
    @PreAuthorize("hasRole('HR')")
    @PutMapping("/{id}/review")
    public ResponseEntity<DataResponse> reviewApplication(@PathVariable Integer id,
                                                          @Valid @RequestBody ApplicationReviewRequest request) {
        ApplicationResponse response = applicationService.reviewApplication(id, request);
        return ResponseEntity.ok(DataResponse.builder()
                .message("Application reviewed successfully")
                .data(response)
                .build());
    }

    // Manual fallback: EmployeeService now advances workflow through Kafka
    @Operation(
            summary = "Advance workflow step",
            description = "Manual fallback/test endpoint. The production EmployeeService integration now uses Kafka topic visa-workflow-events instead of Feign. Updates currentStep, comment, and lastModificationDate while status remains Open."
    )
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'HR')")
    @PatchMapping("/{id}/advance")
    public ResponseEntity<DataResponse> advanceWorkflow(@PathVariable Integer id,
                                                        @Valid @RequestBody AdvanceWorkflowRequest request) {
        ApplicationResponse response = applicationService.advanceWorkflow(id, request);
        return ResponseEntity.ok(DataResponse.builder()
                .message("Application workflow advanced successfully")
                .data(response)
                .build());
    }
}
