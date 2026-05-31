package bf.backend.employee_service.service.impl;

import bf.backend.employee_service.client.ApplicationServiceClient;
import bf.backend.employee_service.dto.request.ApplicationDocumentMetadataRequest;
import bf.backend.employee_service.dto.request.DocumentUploadMetadata;
import bf.backend.employee_service.dto.response.DocumentResponse;
import bf.backend.employee_service.entity.Employee;
import bf.backend.employee_service.entity.PersonalDocument;
import bf.backend.employee_service.exception.ResourceNotFoundException;
import bf.backend.employee_service.exception.UnauthorizedActionException;
import bf.backend.employee_service.exception.ValidationException;
import bf.backend.employee_service.mapper.DocumentMapper;
import bf.backend.employee_service.repository.EmployeeRepository;
import bf.backend.employee_service.repository.PersonalDocumentRepository;
import bf.backend.employee_service.service.DocumentService;
import bf.backend.employee_service.service.FileStorageService;
import bf.backend.employee_service.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final String TEMPLATE_PREFIX = "templates/";
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf", "image/jpeg", "image/png");

    private final EmployeeRepository employeeRepository;
    private final PersonalDocumentRepository personalDocumentRepository;
    private final FileStorageService fileStorageService;
    private final ApplicationServiceClient applicationServiceClient;

    @Override
    public DocumentResponse uploadDocument(MultipartFile file, DocumentUploadMetadata meta) {
        validateFile(file);
        Employee employee = findEmployeeByCurrentUser();

        String s3Key = fileStorageService.upload(file, "documents/" + employee.getId());

        PersonalDocument doc = DocumentMapper.toEntity(meta, s3Key, employee);
        PersonalDocument saved = personalDocumentRepository.save(doc);
        try {
            applicationServiceClient.createDocumentMetadata(
                    ApplicationDocumentMetadataRequest.builder()
                            .applicationId(meta.applicationId())
                            .type(meta.documentType().name())
                            .isRequired(1)
                            .path(s3Key)
                            .sourceDocumentId(saved.getId())
                            .title(meta.title())
                            .build()
            );
        } catch (Exception e) {
            System.err.println("Failed to sync document metadata to Application Service. applicationId="
                    + meta.applicationId() + ", title=" + meta.title());
        }
        return DocumentMapper.toResponse(saved);
    }

    @Override
    public DocumentResponse getDocumentById(String documentId) {
        return DocumentMapper.toResponse(findDocumentWithAccess(documentId));
    }

    @Override
    public List<DocumentResponse> getMyDocuments() {
        Employee employee = findEmployeeByCurrentUser();
        return personalDocumentRepository.findByEmployeeIdOrderByCreatedAtDesc(employee.getId())
                .stream()
                .map(DocumentMapper::toResponse)
                .toList();
    }

    @Override
    public List<DocumentResponse> getDocumentsByEmployeeId(String employeeId) {
        if (!SecurityUtils.isHr()) {
            Employee current = findEmployeeByCurrentUser();
            if (!current.getId().equals(employeeId)) {
                throw new UnauthorizedActionException(
                        "Access denied to documents for employee: " + employeeId);
            }
        }
        return personalDocumentRepository.findByEmployeeIdOrderByCreatedAtDesc(employeeId)
                .stream()
                .map(DocumentMapper::toResponse)
                .toList();
    }

    @Override
    public byte[] downloadDocument(String documentId) {
        PersonalDocument doc = findDocumentWithAccess(documentId);
        return fileStorageService.download(doc.getS3Key());
    }

    @Override
    public String getPresignedUrl(String documentId) {
        PersonalDocument doc = findDocumentWithAccess(documentId);
        return fileStorageService.generatePresignedDownloadUrl(doc.getS3Key(), Duration.ofMinutes(15));
    }

    @Override
    public String getTemplatePresignedUrl(String templateKey) {
        String normalizedKey = normalizeTemplateKey(templateKey);
        return fileStorageService.generatePresignedDownloadUrl(normalizedKey, Duration.ofMinutes(15));
    }

    @Override
    public DocumentResponse addHrComment(String documentId, String comment) {
        PersonalDocument doc = personalDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));
        doc.setComment(comment);
        return DocumentMapper.toResponse(personalDocumentRepository.save(doc));
    }

    @Override
    public void deleteDocument(String documentId) {
        PersonalDocument doc = findDocumentWithAccess(documentId);
        fileStorageService.delete(doc.getS3Key());
        personalDocumentRepository.delete(doc);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Employee findEmployeeByCurrentUser() {
        Long userId = SecurityUtils.getCurrentUserId();
        return employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No employee profile found for current user."));
    }

    private PersonalDocument findDocumentWithAccess(String documentId) {
        PersonalDocument doc = personalDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));
        if (!SecurityUtils.isHr()) {
            Employee current = findEmployeeByCurrentUser();
            if (!current.getId().equals(doc.getEmployeeId())) {
                throw new UnauthorizedActionException("Access denied to document: " + documentId);
            }
        }
        return doc;
    }

    private static void validateFile(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ValidationException(
                    "File size " + (file.getSize() / (1024 * 1024)) + " MB exceeds the 10 MB limit.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new ValidationException(
                    "Unsupported file type '" + contentType + "'. Only PDF, JPG, and PNG are allowed.");
        }
    }

    private static String normalizeTemplateKey(String templateKey) {
        if (templateKey == null || templateKey.isBlank()) {
            throw new ValidationException("Template key is required.");
        }

        String normalizedKey = templateKey.trim();
        while (normalizedKey.startsWith("/")) {
            normalizedKey = normalizedKey.substring(1);
        }

        if (!normalizedKey.startsWith(TEMPLATE_PREFIX)
                || normalizedKey.contains("..")
                || normalizedKey.endsWith("/")) {
            throw new ValidationException("Invalid template key: " + templateKey);
        }

        return normalizedKey;
    }
}
