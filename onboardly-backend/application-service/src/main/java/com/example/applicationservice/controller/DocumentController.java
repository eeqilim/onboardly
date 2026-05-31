package com.example.applicationservice.controller;

import com.example.applicationservice.dto.request.DocumentUploadRequest;
import com.example.applicationservice.dto.response.DataResponse;
import com.example.applicationservice.dto.response.DocumentResponse;
import com.example.applicationservice.dto.response.DocumentTemplateResponse;
import com.example.applicationservice.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/application/documents")
@RequiredArgsConstructor
@Tag(
        name = "Application Documents",
        description = "Manage application document metadata used by ApplicationService. File storage/S3 may be owned by EmployeeService."
)
public class DocumentController {

    private final DocumentService documentService;

    // Employee + HR: get backend-managed template documents
    @Operation(
            summary = "Get template documents by application type",
            description = "Returns required/optional template documents such as W4, I9, COMPANY_POLICY, and DIRECT_DEPOSIT."
    )
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'HR')")
    @GetMapping("/templates")
    public ResponseEntity<DataResponse> getDocumentTemplates(@RequestParam String applicationType) {
        List<DocumentTemplateResponse> response = documentService.getDocumentTemplates(applicationType);
        return ResponseEntity.ok(DataResponse.builder()
                .message("Document templates retrieved successfully")
                .data(response)
                .build());
    }

    // Employee: upload a document
    @Operation(
            summary = "Create document metadata",
            description = "Stores document metadata for an application workflow. This does not upload the binary file to S3."
    )
    @PreAuthorize("hasRole('EMPLOYEE')")
    @PostMapping
    public ResponseEntity<DataResponse> createDocumentMetadata(@Valid @RequestBody DocumentUploadRequest request) {
        DocumentResponse response = documentService.createDocumentMetadata(request);
        return ResponseEntity.ok(DataResponse.builder()
                .message("Document uploaded successfully")
                .data(response)
                .build());
    }

    // Employee + HR: get document by id
    @Operation(
            summary = "Get document metadata by ID",
            description = "Returns one digital document metadata record by document ID."
    )
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'HR')")
    @GetMapping("/{id}")
    public ResponseEntity<DataResponse> getDocumentById(@PathVariable Integer id) {
        DocumentResponse response = documentService.getDocumentById(id);
        return ResponseEntity.ok(DataResponse.builder()
                .message("Document retrieved successfully")
                .data(response)
                .build());
    }

    // Employee + HR: get all documents by applicationId
    @Operation(
            summary = "Get documents by application ID",
            description = "Returns all document metadata records linked to one application workflow."
    )
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'HR')")
    @GetMapping("/application/{applicationId}")
    public ResponseEntity<DataResponse> getDocumentsByApplicationId(@PathVariable Integer applicationId) {
        List<DocumentResponse> response = documentService.getDocumentsByApplicationId(applicationId);
        return ResponseEntity.ok(DataResponse.builder()
                .message("Documents retrieved successfully")
                .data(response)
                .build());
    }

    // Employee + HR: get documents by applicationId and type
    @Operation(
            summary = "Get documents by application ID and type",
            description = "Returns document metadata for one application filtered by document type, such as WORK_AUTH or I_20."
    )
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'HR')")
    @GetMapping("/application/{applicationId}/type/{type}")
    public ResponseEntity<DataResponse> getDocumentsByApplicationIdAndType(@PathVariable Integer applicationId,
                                                                           @PathVariable String type) {
        List<DocumentResponse> response = documentService.getDocumentsByApplicationIdAndType(applicationId, type);
        return ResponseEntity.ok(DataResponse.builder()
                .message("Documents retrieved successfully")
                .data(response)
                .build());
    }

    // HR: get documents by employee auth userId and application type
    @Operation(
            summary = "Get documents by employee and application type",
            description = "Returns document metadata for workflows belonging to one employee auth userId and application type, such as ONBOARDING or OPT_STEM."
    )
    @PreAuthorize("hasRole('HR')")
    @GetMapping("/employee/{employeeId}/application-type/{applicationType}")
    public ResponseEntity<DataResponse> getDocumentsByEmployeeIdAndApplicationType(@PathVariable Long employeeId,
                                                                                   @PathVariable String applicationType) {
        List<DocumentResponse> response = documentService
                .getDocumentsByEmployeeIdAndApplicationType(employeeId, applicationType);
        return ResponseEntity.ok(DataResponse.builder()
                .message("Documents retrieved successfully")
                .data(response)
                .build());
    }

    // Employee: get documents by current user and application type
    @Operation(
            summary = "Get documents for current user by application type",
            description = "Returns document metadata for workflows belonging to the current employee and specified application type, such as ONBOARDING or OPT_STEM."
    )
    @PreAuthorize("hasRole('EMPLOYEE')")
    @GetMapping("/me/application-type/{type}")
    public ResponseEntity<DataResponse> getDocumentsByCurrentUserAndApplicationType(@PathVariable String type) {
        List<DocumentResponse> response = documentService.getDocumentsByCurrentUserAndApplicationType(type);
        return ResponseEntity.ok(DataResponse.builder()
                .message("Documents retrieved successfully")
                .data(response)
                .build());
    }

    // Employee: check all required documents before submission
    @Operation(
            summary = "Check required documents",
            description = "Validates that required document metadata exists for an application before the employee submits the workflow."
    )
    @PreAuthorize("hasRole('EMPLOYEE')")
    @GetMapping("/application/{applicationId}/check")
    public ResponseEntity<DataResponse> checkRequiredDocuments(@PathVariable Integer applicationId) {
        documentService.checkRequiredDocuments(applicationId);
        return ResponseEntity.ok(DataResponse.builder()
                .message("All required documents are uploaded")
                .data(null)
                .build());
    }
}
