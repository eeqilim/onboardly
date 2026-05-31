package bf.backend.employee_service.controller;

import bf.backend.employee_service.dto.request.DocumentUploadMetadata;
import bf.backend.employee_service.dto.request.HrCommentRequest;
import bf.backend.employee_service.dto.response.DocumentResponse;
import bf.backend.employee_service.service.DocumentService;
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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/employee/documents")
@RequiredArgsConstructor
@Tag(name = "Documents", description = "Personal document upload, download, and HR review")
@SecurityRequirement(name = "BearerAuth")
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a document (PDF, JPG, or PNG; max 10 MB)")
    @ApiResponse(responseCode = "200", description = "Document uploaded and persisted")
    @ApiResponse(responseCode = "400", description = "File too large or unsupported type")
    public ResponseEntity<DocumentResponse> uploadDocument(
            @RequestParam MultipartFile file,
            @RequestPart @Valid DocumentUploadMetadata meta) {
        return ResponseEntity.ok(documentService.uploadDocument(file, meta));
    }

    @GetMapping("/me")
    @Operation(summary = "List all documents belonging to the current user")
    @ApiResponse(responseCode = "200", description = "Document list returned")
    public ResponseEntity<List<DocumentResponse>> getMyDocuments() {
        return ResponseEntity.ok(documentService.getMyDocuments());
    }

    @GetMapping("/employee/{employeeId}")
    @Operation(summary = "List documents for a specific employee — HR sees any, employee sees own only")
    @ApiResponse(responseCode = "200", description = "Document list returned")
    @ApiResponse(responseCode = "403", description = "Not owner and not HR")
    public ResponseEntity<List<DocumentResponse>> getDocumentsByEmployeeId(
            @PathVariable String employeeId) {
        return ResponseEntity.ok(documentService.getDocumentsByEmployeeId(employeeId));
    }

    @GetMapping("/{documentId}/download")
    @Operation(summary = "Download a document as a file attachment")
    @ApiResponse(responseCode = "200", description = "File bytes returned with Content-Disposition header")
    @ApiResponse(responseCode = "403", description = "Not owner and not HR")
    @ApiResponse(responseCode = "404", description = "Document not found")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable String documentId) {
        DocumentResponse meta = documentService.getDocumentById(documentId);
        byte[] data = documentService.downloadDocument(documentId);

        String displayName = extractDisplayFilename(meta.s3Key());
        String contentType = inferContentType(displayName);

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + displayName + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(data);
    }

    @GetMapping("/{documentId}/preview-url")
    @Operation(summary = "Get a 15-minute presigned S3 URL for inline document preview")
    @ApiResponse(responseCode = "200", description = "Presigned URL returned")
    @ApiResponse(responseCode = "403", description = "Not owner and not HR")
    @ApiResponse(responseCode = "404", description = "Document not found")
    public ResponseEntity<Map<String, String>> getPresignedUrl(@PathVariable String documentId) {
        return ResponseEntity.ok(Map.of("url", documentService.getPresignedUrl(documentId)));
    }

    @GetMapping("/template-preview-url")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'HR')")
    @Operation(summary = "Get a 15-minute presigned S3 URL for a template document")
    @ApiResponse(responseCode = "200", description = "Presigned template URL returned")
    @ApiResponse(responseCode = "400", description = "Invalid template key")
    public ResponseEntity<Map<String, String>> getTemplatePresignedUrl(@RequestParam String key) {
        return ResponseEntity.ok(Map.of("url", documentService.getTemplatePresignedUrl(key)));
    }

    @PostMapping("/{documentId}/hr-comment")
    @PreAuthorize("hasRole('HR')")
    @Operation(summary = "Add or replace HR comment on a document — HR only")
    @ApiResponse(responseCode = "200", description = "Comment saved")
    @ApiResponse(responseCode = "400", description = "Blank comment")
    @ApiResponse(responseCode = "403", description = "HR role required")
    @ApiResponse(responseCode = "404", description = "Document not found")
    public ResponseEntity<DocumentResponse> addHrComment(
            @PathVariable String documentId,
            @RequestBody @Valid HrCommentRequest req) {
        return ResponseEntity.ok(documentService.addHrComment(documentId, req.comment()));
    }

    @DeleteMapping("/{documentId}")
    @Operation(summary = "Delete a document from S3 and the database — owner or HR")
    @ApiResponse(responseCode = "204", description = "Document deleted")
    @ApiResponse(responseCode = "403", description = "Not owner and not HR")
    @ApiResponse(responseCode = "404", description = "Document not found")
    public ResponseEntity<Void> deleteDocument(@PathVariable String documentId) {
        documentService.deleteDocument(documentId);
        return ResponseEntity.noContent().build();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    // Key format: {prefix}/{UUID}-{originalFilename}  (UUID is 36 chars)
    private static String extractDisplayFilename(String s3Key) {
        String keyFilename = s3Key.substring(s3Key.lastIndexOf('/') + 1);
        return keyFilename.length() > 37 ? keyFilename.substring(37) : keyFilename;
    }

    private static String inferContentType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        return "application/octet-stream";
    }
}
