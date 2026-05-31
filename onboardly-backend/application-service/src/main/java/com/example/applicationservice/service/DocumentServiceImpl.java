package com.example.applicationservice.service;

import com.example.applicationservice.domain.ApplicationWorkFlow;
import com.example.applicationservice.domain.DigitalDocument;
import com.example.applicationservice.dto.request.DocumentUploadRequest;
import com.example.applicationservice.dto.response.DocumentResponse;
import com.example.applicationservice.dto.response.DocumentTemplateResponse;
import com.example.applicationservice.exception.ApplicationNotFoundException;
import com.example.applicationservice.exception.DocumentNotFoundException;
import com.example.applicationservice.exception.RequiredDocumentMissingException;
import com.example.applicationservice.repository.ApplicationWorkFlowRepository;
import com.example.applicationservice.repository.DigitalDocumentRepository;
import com.example.applicationservice.security.AuthUserDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DigitalDocumentRepository documentRepository;
    private final ApplicationWorkFlowRepository applicationRepository;

    @Override
    @Transactional
    public DocumentResponse createDocumentMetadata(DocumentUploadRequest request) {
        ApplicationWorkFlow application = applicationRepository.findById(request.getApplicationId())
                .orElseThrow(() -> new ApplicationNotFoundException(
                        "Application not found with id: " + request.getApplicationId()));
        String normalizedType = normalizeType(request.getType());

        DigitalDocument document = documentRepository
                .findFirstByApplicationWorkFlowIdAndType(request.getApplicationId(), normalizedType)
                .orElseGet(() -> DigitalDocument.builder()
                        .applicationWorkFlow(application)
                        .type(normalizedType)
                        .build());

        document.setApplicationWorkFlow(application);
        document.setType(normalizedType);
        document.setIsRequired(request.getIsRequired());
        document.setPath(request.getPath());
        document.setSourceDocumentId(request.getSourceDocumentId());
        if (request.getDescription() != null && !request.getDescription().isBlank()) {
            document.setDescription(request.getDescription());
        }
        document.setTitle(request.getTitle());

        return toDocumentResponse(documentRepository.save(document));
    }

    @Override
    public DocumentResponse getDocumentById(Integer id) {
        DigitalDocument document = documentRepository.findById(id)
                .orElseThrow(() -> new DocumentNotFoundException(
                        "Document not found with id: " + id));
        return toDocumentResponse(document);
    }

    @Override
    public List<DocumentResponse> getDocumentsByApplicationId(Integer applicationId) {
        return documentRepository.findByApplicationWorkFlowId(applicationId)
                .stream()
                .map(this::toDocumentResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<DocumentResponse> getDocumentsByApplicationIdAndType(Integer applicationId, String type) {
        return documentRepository.findByApplicationWorkFlowIdAndType(applicationId, normalizeType(type))
                .stream()
                .map(this::toDocumentResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<DocumentResponse> getDocumentsByEmployeeIdAndApplicationType(Long employeeId, String applicationType) {
        return documentRepository
                .findByApplicationWorkFlowEmployeeIdAndApplicationWorkFlowApplicationType(employeeId, applicationType)
                .stream()
                .map(this::toDocumentResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<DocumentResponse> getDocumentsByCurrentUserAndApplicationType(String applicationType) {
        Long currentUserId = getCurrentUserId();
        return documentRepository
                .findByApplicationWorkFlowEmployeeIdAndApplicationWorkFlowApplicationType(currentUserId, applicationType)
                .stream()
                .map(this::toDocumentResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void checkRequiredDocuments(Integer applicationId) {
        List<DigitalDocument> requiredDocs = documentRepository
                .findByApplicationWorkFlowIdAndIsRequired(applicationId, 1);

        if (requiredDocs.isEmpty()) {
            throw new RequiredDocumentMissingException(
                    "No required documents found for application: " + applicationId);
        }

        boolean hasMissingRequiredDocument = requiredDocs.stream()
                .anyMatch(doc -> doc.getPath() == null || doc.getPath().isBlank());

        if (hasMissingRequiredDocument) {
            throw new RequiredDocumentMissingException(
                    "Some required documents are missing for application: " + applicationId);
        }
    }

    @Override
    public List<DocumentTemplateResponse> getDocumentTemplates(String applicationType) {
        return DocumentTemplateCatalog.forApplicationType(applicationType)
                .stream()
                .map(DocumentTemplateCatalog.TemplateSpec::toResponse)
                .toList();
    }

    private Long getCurrentUserId() {
        AuthUserDetail userDetail = (AuthUserDetail) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return userDetail.getUserId();
    }

    private String normalizeType(String type) {
        return type == null ? null : type.trim().toUpperCase();
    }

    private DocumentResponse toDocumentResponse(DigitalDocument document) {
        return DocumentResponse.builder()
                .id(document.getId())
                .applicationId(document.getApplicationWorkFlow().getId())
                .type(document.getType())
                .isRequired(document.getIsRequired())
                .path(document.getPath())
                .sourceDocumentId(document.getSourceDocumentId())
                .description(document.getDescription())
                .title(document.getTitle())
                .build();
    }
}
