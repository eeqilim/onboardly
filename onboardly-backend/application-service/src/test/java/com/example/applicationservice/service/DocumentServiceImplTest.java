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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentServiceImplTest {

    @Mock
    private DigitalDocumentRepository documentRepository;

    @Mock
    private ApplicationWorkFlowRepository applicationRepository;

    @InjectMocks
    private DocumentServiceImpl documentService;

    @Test
    void uploadDocument_createsDocumentForApplication() {
        ApplicationWorkFlow application = application(10);
        DocumentUploadRequest request = DocumentUploadRequest.builder()
                .applicationId(application.getId())
                .type(" work_auth ")
                .isRequired(1)
                .path("s3://bucket/work-auth.pdf")
                .sourceDocumentId("personal-doc-123")
                .title("Work Authorization")
                .description("EAD card")
                .build();
        DigitalDocument savedDocument = document(20, application, "WORK_AUTH", request.getIsRequired());
        savedDocument.setSourceDocumentId(request.getSourceDocumentId());

        when(applicationRepository.findById(application.getId())).thenReturn(Optional.of(application));
        when(documentRepository.findFirstByApplicationWorkFlowIdAndType(application.getId(), "WORK_AUTH"))
                .thenReturn(Optional.empty());
        when(documentRepository.save(any(DigitalDocument.class))).thenReturn(savedDocument);

        DocumentResponse response = documentService.createDocumentMetadata(request);

        assertThat(response.getId()).isEqualTo(20);
        assertThat(response.getApplicationId()).isEqualTo(10);
        assertThat(response.getType()).isEqualTo("WORK_AUTH");
        assertThat(response.getSourceDocumentId()).isEqualTo("personal-doc-123");

        ArgumentCaptor<DigitalDocument> captor = ArgumentCaptor.forClass(DigitalDocument.class);
        verify(documentRepository).save(captor.capture());
        assertThat(captor.getValue().getApplicationWorkFlow()).isSameAs(application);
        assertThat(captor.getValue().getType()).isEqualTo("WORK_AUTH");
        assertThat(captor.getValue().getSourceDocumentId()).isEqualTo("personal-doc-123");
        assertThat(captor.getValue().getTitle()).isEqualTo("Work Authorization");
    }

    @Test
    void uploadDocument_throwsWhenApplicationIsMissing() {
        DocumentUploadRequest request = DocumentUploadRequest.builder()
                .applicationId(99)
                .type("WORK_AUTH")
                .isRequired(1)
                .path("s3://bucket/work-auth.pdf")
                .title("Work Authorization")
                .build();

        when(applicationRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.createDocumentMetadata(request))
                .isInstanceOf(ApplicationNotFoundException.class)
                .hasMessageContaining("Application not found");
    }

    @Test
    void uploadDocument_preservesExistingDescriptionWhenRequestDescriptionIsBlank() {
        ApplicationWorkFlow application = application(10);
        DigitalDocument existingDocument = document(20, application, "W4", 1);
        existingDocument.setDescription("Required onboarding tax form");
        DocumentUploadRequest request = DocumentUploadRequest.builder()
                .applicationId(application.getId())
                .type("W4")
                .isRequired(1)
                .path("s3://bucket/w4.pdf")
                .sourceDocumentId("personal-doc-123")
                .title("W-4 Form")
                .description(" ")
                .build();

        when(applicationRepository.findById(application.getId())).thenReturn(Optional.of(application));
        when(documentRepository.findFirstByApplicationWorkFlowIdAndType(application.getId(), "W4"))
                .thenReturn(Optional.of(existingDocument));
        when(documentRepository.save(any(DigitalDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DocumentResponse response = documentService.createDocumentMetadata(request);

        assertThat(response.getDescription()).isEqualTo("Required onboarding tax form");
        assertThat(existingDocument.getPath()).isEqualTo("s3://bucket/w4.pdf");
        assertThat(existingDocument.getSourceDocumentId()).isEqualTo("personal-doc-123");
    }

    @Test
    void getDocumentById_returnsDocument() {
        ApplicationWorkFlow application = application(10);
        DigitalDocument document = document(20, application, "DRIVER_LICENSE", 1);

        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));

        DocumentResponse response = documentService.getDocumentById(document.getId());

        assertThat(response.getId()).isEqualTo(20);
        assertThat(response.getApplicationId()).isEqualTo(10);
        assertThat(response.getType()).isEqualTo("DRIVER_LICENSE");
    }

    @Test
    void getDocumentById_throwsWhenMissing() {
        when(documentRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.getDocumentById(99))
                .isInstanceOf(DocumentNotFoundException.class)
                .hasMessageContaining("Document not found");
    }

    @Test
    void getDocumentsByApplicationId_returnsDocuments() {
        ApplicationWorkFlow application = application(10);
        DigitalDocument document = document(20, application, "DRIVER_LICENSE", 1);

        when(documentRepository.findByApplicationWorkFlowId(application.getId()))
                .thenReturn(List.of(document));

        List<DocumentResponse> response = documentService.getDocumentsByApplicationId(application.getId());

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getApplicationId()).isEqualTo(10);
    }

    @Test
    void getDocumentsByApplicationIdAndType_returnsMatchingDocuments() {
        ApplicationWorkFlow application = application(10);
        DigitalDocument document = document(20, application, "I_983", 1);

        when(documentRepository.findByApplicationWorkFlowIdAndType(application.getId(), "I_983"))
                .thenReturn(List.of(document));

        List<DocumentResponse> response = documentService.getDocumentsByApplicationIdAndType(application.getId(), " i_983 ");

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getType()).isEqualTo("I_983");
    }

    @Test
    void getDocumentsByEmployeeIdAndApplicationType_returnsMatchingDocuments() {
        ApplicationWorkFlow application = application(10);
        application.setEmployeeId(4L);
        application.setApplicationType("OPT_STEM");
        DigitalDocument document = document(20, application, "I_20", 1);

        when(documentRepository.findByApplicationWorkFlowEmployeeIdAndApplicationWorkFlowApplicationType(4L, "OPT_STEM"))
                .thenReturn(List.of(document));

        List<DocumentResponse> response = documentService
                .getDocumentsByEmployeeIdAndApplicationType(4L, "OPT_STEM");

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getApplicationId()).isEqualTo(10);
        assertThat(response.get(0).getType()).isEqualTo("I_20");
    }

    @Test
    void getDocumentTemplates_returnsOnboardingTemplates() {
        List<DocumentTemplateResponse> response = documentService.getDocumentTemplates(" onboarding ");

        assertThat(response).extracting(DocumentTemplateResponse::getType)
                .containsExactly("W4", "I9", "COMPANY_POLICY", "DIRECT_DEPOSIT");
        assertThat(response).extracting(DocumentTemplateResponse::getApplicationType)
                .containsOnly("ONBOARDING");
        assertThat(response).filteredOn(DocumentTemplateResponse::getType, "W4")
                .singleElement()
                .satisfies(template -> {
                    assertThat(template.getTitle()).isEqualTo("W-4 Form");
                    assertThat(template.getIsRequired()).isEqualTo(1);
                    assertThat(template.getPath()).isEqualTo("templates/onboarding/fw4.pdf");
                });
    }

    @Test
    void getDocumentTemplates_returnsOptStemTemplate() {
        List<DocumentTemplateResponse> response = documentService.getDocumentTemplates("OPT_STEM");

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getApplicationType()).isEqualTo("OPT_STEM");
        assertThat(response.get(0).getType()).isEqualTo("I_983");
        assertThat(response.get(0).getTitle()).isEqualTo("I-983 Form");
        assertThat(response.get(0).getPath()).isEqualTo("templates/opt-stem/i983.pdf");
    }

    @Test
    void getDocumentTemplates_returnsEmptyListForUnsupportedApplicationType() {
        List<DocumentTemplateResponse> response = documentService.getDocumentTemplates("UNKNOWN");

        assertThat(response).isEmpty();
    }

    @Test
    void checkRequiredDocuments_passesWhenRequiredDocumentsExist() {
        ApplicationWorkFlow application = application(10);
        DigitalDocument document = document(20, application, "WORK_AUTH", 1);

        when(documentRepository.findByApplicationWorkFlowIdAndIsRequired(application.getId(), 1))
                .thenReturn(List.of(document));

        documentService.checkRequiredDocuments(application.getId());

        verify(documentRepository).findByApplicationWorkFlowIdAndIsRequired(application.getId(), 1);
    }

    @Test
    void checkRequiredDocuments_throwsWhenRequiredDocumentsAreMissing() {
        when(documentRepository.findByApplicationWorkFlowIdAndIsRequired(10, 1))
                .thenReturn(List.of());

        assertThatThrownBy(() -> documentService.checkRequiredDocuments(10))
                .isInstanceOf(RequiredDocumentMissingException.class)
                .hasMessageContaining("No required documents found");
    }

    private ApplicationWorkFlow application(Integer id) {
        return ApplicationWorkFlow.builder()
                .id(id)
                .employeeId(1L)
                .status("Open")
                .applicationType("ONBOARDING")
                .createDate(LocalDateTime.now())
                .lastModificationDate(LocalDateTime.now())
                .build();
    }

    private DigitalDocument document(Integer id, ApplicationWorkFlow application, String type, Integer isRequired) {
        return DigitalDocument.builder()
                .id(id)
                .applicationWorkFlow(application)
                .type(type)
                .isRequired(isRequired)
                .path("s3://bucket/document.pdf")
                .description("Document description")
                .title("Document title")
                .build();
    }
}
