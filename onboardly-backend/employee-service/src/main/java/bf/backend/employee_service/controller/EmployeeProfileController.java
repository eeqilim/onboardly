package bf.backend.employee_service.controller;

import bf.backend.employee_service.dto.request.*;
import bf.backend.employee_service.dto.response.EmployeeFullProfileResponse;
import bf.backend.employee_service.dto.response.EmployeeProfileResponse;
import bf.backend.employee_service.service.EmployeeProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/employee/profile")
@RequiredArgsConstructor
@Tag(name = "Employee Profile", description = "Personal profile management for employees and HR")
@SecurityRequirement(name = "BearerAuth")
public class EmployeeProfileController {

    private final EmployeeProfileService employeeProfileService;

    @GetMapping("/me")
    @Operation(summary = "Get the authenticated user's own profile (SSN masked)")
    @ApiResponse(responseCode = "200", description = "Profile returned")
    @ApiResponse(responseCode = "404", description = "No employee record found for current user")
    public ResponseEntity<EmployeeProfileResponse> getMyProfile() {
        return ResponseEntity.ok(employeeProfileService.getMyProfile());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get employee profile by ID (SSN masked; HR sees any, employee sees own only)")
    @ApiResponse(responseCode = "200", description = "Profile returned")
    @ApiResponse(responseCode = "403", description = "Not the profile owner and not HR")
    @ApiResponse(responseCode = "404", description = "Employee not found")
    public ResponseEntity<EmployeeProfileResponse> getProfileById(@PathVariable String id) {
        return ResponseEntity.ok(employeeProfileService.getProfileById(id));
    }

    @GetMapping("/{id}/full")
    @PreAuthorize("hasRole('HR')")
    @Operation(summary = "Get full profile with unmasked SSN — HR only")
    @ApiResponse(responseCode = "200", description = "Full profile returned")
    @ApiResponse(responseCode = "403", description = "HR role required")
    @ApiResponse(responseCode = "404", description = "Employee not found")
    public ResponseEntity<EmployeeFullProfileResponse> getFullProfileById(@PathVariable String id) {
        return ResponseEntity.ok(employeeProfileService.getFullProfileById(id));
    }

    @PutMapping("/{id}/personal")
    @Operation(summary = "Update personal contact info (email, cell phone, alternate phone, gender, date of birth)")
    @ApiResponse(responseCode = "200", description = "Profile updated")
    @ApiResponse(responseCode = "400", description = "Validation error")
    @ApiResponse(responseCode = "403", description = "Not the profile owner and not HR")
    @ApiResponse(responseCode = "404", description = "Employee not found")
    public ResponseEntity<EmployeeProfileResponse> updatePersonal(
            @PathVariable String id,
            @RequestBody @Valid UpdatePersonalRequest req) {
        return ResponseEntity.ok(employeeProfileService.updatePersonal(id, req));
    }

    @PostMapping("/{id}/contact")
    @Operation(summary = "Add a new emergency contact")
    @ApiResponse(responseCode = "200", description = "Contact added")
    @ApiResponse(responseCode = "400", description = "Type must be EMERGENCY")
    @ApiResponse(responseCode = "403", description = "Not the profile owner and not HR")
    @ApiResponse(responseCode = "404", description = "Employee not found")
    public ResponseEntity<EmployeeProfileResponse> addContact(
            @PathVariable String id,
            @RequestBody @Valid ContactRequest req) {
        return ResponseEntity.ok(employeeProfileService.addContact(id, req));
    }

    @PutMapping("/{id}/contact/{contactId}")
    @Operation(summary = "Update a specific contact by its ID")
    @ApiResponse(responseCode = "200", description = "Contact updated")
    @ApiResponse(responseCode = "403", description = "Not the profile owner and not HR")
    @ApiResponse(responseCode = "404", description = "Employee or contact not found")
    public ResponseEntity<EmployeeProfileResponse> updateContactById(
            @PathVariable String id,
            @PathVariable String contactId,
            @RequestBody @Valid ContactRequest req) {
        return ResponseEntity.ok(employeeProfileService.updateContactById(id, contactId, req));
    }

    @DeleteMapping("/{id}/contact/{contactId}")
    @Operation(summary = "Delete a specific contact by its ID")
    @ApiResponse(responseCode = "200", description = "Contact deleted")
    @ApiResponse(responseCode = "403", description = "Not the profile owner and not HR")
    @ApiResponse(responseCode = "404", description = "Employee or contact not found")
    public ResponseEntity<EmployeeProfileResponse> deleteContactById(
            @PathVariable String id,
            @PathVariable String contactId) {
        return ResponseEntity.ok(employeeProfileService.deleteContactById(id, contactId));
    }

    @PutMapping("/{id}/name")
    @Operation(summary = "Update name fields (first, last, middle, preferred)")
    @ApiResponse(responseCode = "200", description = "Profile updated")
    @ApiResponse(responseCode = "400", description = "Validation error")
    @ApiResponse(responseCode = "403", description = "Not the profile owner and not HR")
    @ApiResponse(responseCode = "404", description = "Employee not found")
    public ResponseEntity<EmployeeProfileResponse> updateName(
            @PathVariable String id,
            @RequestBody @Valid UpdateNameRequest req) {
        return ResponseEntity.ok(employeeProfileService.updateName(id, req));
    }

    @PutMapping("/{id}/address")
    @Operation(summary = "Upsert address by type — creates if absent, replaces if present")
    @ApiResponse(responseCode = "200", description = "Profile updated")
    @ApiResponse(responseCode = "400", description = "Validation error")
    @ApiResponse(responseCode = "403", description = "Not the profile owner and not HR")
    @ApiResponse(responseCode = "404", description = "Employee not found")
    public ResponseEntity<EmployeeProfileResponse> updateAddress(
            @PathVariable String id,
            @RequestBody @Valid UpdateAddressRequest req) {
        return ResponseEntity.ok(employeeProfileService.updateAddress(id, req));
    }

    @PutMapping("/{id}/contact")
    @Operation(summary = "Upsert contact by type — for REFERENCE replaces the single entry; for EMERGENCY updates the first or adds one")
    @ApiResponse(responseCode = "200", description = "Profile updated")
    @ApiResponse(responseCode = "400", description = "Validation error")
    @ApiResponse(responseCode = "403", description = "Not the profile owner and not HR")
    @ApiResponse(responseCode = "404", description = "Employee not found")
    public ResponseEntity<EmployeeProfileResponse> updateContact(
            @PathVariable String id,
            @RequestBody @Valid UpdateContactRequest req) {
        return ResponseEntity.ok(employeeProfileService.updateContact(id, req));
    }

    @PostMapping(value = "/{id}/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload or replace the employee's avatar image (JPG, PNG; max 5 MB)")
    @ApiResponse(responseCode = "200", description = "Avatar uploaded; presigned URL returned")
    @ApiResponse(responseCode = "403", description = "Not the profile owner and not HR")
    @ApiResponse(responseCode = "404", description = "Employee not found")
    public ResponseEntity<Map<String, String>> uploadAvatar(
            @PathVariable String id,
            @RequestParam MultipartFile file) {
        String presignedUrl = employeeProfileService.uploadAvatar(id, file);
        return ResponseEntity.ok(Map.of("url", presignedUrl));
    }

    @PutMapping("/{id}/employment")
    @Operation(summary = "Update employment fields (start/end dates, driver licence, house)")
    @ApiResponse(responseCode = "200", description = "Profile updated")
    @ApiResponse(responseCode = "403", description = "Not the profile owner and not HR")
    @ApiResponse(responseCode = "404", description = "Employee not found")
    public ResponseEntity<EmployeeProfileResponse> updateEmployment(
            @PathVariable String id,
            @RequestBody @Valid UpdateEmploymentRequest req) {
        return ResponseEntity.ok(employeeProfileService.updateEmployment(id, req));
    }
}
