package bf.backend.employee_service.service;

import bf.backend.employee_service.dto.request.DocumentUploadMetadata;
import bf.backend.employee_service.dto.response.DocumentResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DocumentService {

    DocumentResponse uploadDocument(MultipartFile file, DocumentUploadMetadata meta);

    DocumentResponse getDocumentById(String documentId);

    List<DocumentResponse> getMyDocuments();

    List<DocumentResponse> getDocumentsByEmployeeId(String employeeId);

    byte[] downloadDocument(String documentId);

    String getPresignedUrl(String documentId);

    String getTemplatePresignedUrl(String templateKey);

    DocumentResponse addHrComment(String documentId, String comment);

    void deleteDocument(String documentId);
}
