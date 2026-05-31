package bf.backend.employee_service.service.impl;

import bf.backend.employee_service.client.ApplicationServiceClient;
import bf.backend.employee_service.dto.request.ApplicationDocumentMetadataRequest;
import bf.backend.employee_service.dto.request.ReviewApplicationRequest;
import bf.backend.employee_service.dto.response.ApplicationServiceDataResponse;
import bf.backend.employee_service.dto.response.ApplicationWorkflowResponse;
import bf.backend.employee_service.entity.*;
import bf.backend.employee_service.exception.ResourceNotFoundException;
import bf.backend.employee_service.exception.ValidationException;
import bf.backend.employee_service.kafka.event.EmailEvent;
import bf.backend.employee_service.kafka.event.VisaWorkflowEvent;
import bf.backend.employee_service.kafka.producer.EmployeeEventPublisher;
import bf.backend.employee_service.repository.EmployeeRepository;
import bf.backend.employee_service.repository.PersonalDocumentRepository;
import bf.backend.employee_service.repository.VisaStatusRepository;
import bf.backend.employee_service.service.FileStorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VisaStatusServiceImplTest {

    private static final Long USER_ID = 1L;
    private static final String EMPLOYEE_ID = "10";
    private static final String VISA_ID = "200";
    private static final String STEM_OPT_TYPE = "VISA_STEM_OPT";

    @Mock private EmployeeRepository employeeRepository;
    @Mock private VisaStatusRepository visaStatusRepository;
    @Mock private PersonalDocumentRepository personalDocumentRepository;
    @Mock private FileStorageService fileStorageService;
    @Mock private EmployeeEventPublisher employeeEventPublisher;
    @Mock private ApplicationServiceClient applicationServiceClient;

    @InjectMocks
    private VisaStatusServiceImpl service;

    private Employee employee;
    private VisaStatus stemOptVisa;

    @BeforeEach
    void setUp() {
        setAuth(USER_ID, "EMPLOYEE");
        employee = buildEmployee();
        stemOptVisa = buildVisa(VisaType.F1_OPT_STEM);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    // ── getMyActiveVisa ───────────────────────────────────────────────────────

    @Test
    void getMyActiveVisa_returnsActiveVisaForCurrentUser() {
        when(employeeRepository.findByUserId(USER_ID)).thenReturn(Optional.of(employee));
        when(visaStatusRepository.findByEmployeeIdAndActiveFlagTrue(EMPLOYEE_ID))
                .thenReturn(Optional.of(stemOptVisa));

        var result = service.getMyActiveVisa();

        assertThat(result.visaType()).isEqualTo(VisaType.F1_OPT_STEM);
    }

    @Test
    void getMyActiveVisa_throwsWhenNoActiveVisa() {
        when(employeeRepository.findByUserId(USER_ID)).thenReturn(Optional.of(employee));
        when(visaStatusRepository.findByEmployeeIdAndActiveFlagTrue(EMPLOYEE_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMyActiveVisa())
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getMyVisaHistory ─────────────────────────────────────────────────────

    @Test
    void getMyVisaHistory_returnsSortedHistory() {
        VisaStatus old = buildVisa(VisaType.F1_OPT);
        when(employeeRepository.findByUserId(USER_ID)).thenReturn(Optional.of(employee));
        when(visaStatusRepository.findByEmployeeIdOrderByLastModificationDateDesc(EMPLOYEE_ID))
                .thenReturn(List.of(stemOptVisa, old));

        var result = service.getMyVisaHistory();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).visaType()).isEqualTo(VisaType.F1_OPT_STEM);
    }

    // ── getStemOptProgress ───────────────────────────────────────────────────

    @Test
    void getStemOptProgress_returnsNullCurrentStepWhenNothingUploaded() {
        when(employeeRepository.existsById(EMPLOYEE_ID)).thenReturn(true);
        when(personalDocumentRepository.findByEmployeeIdAndApplicationTypeOrderByCreatedAtDesc(
                EMPLOYEE_ID, STEM_OPT_TYPE)).thenReturn(List.of());
        when(visaStatusRepository.findByEmployeeIdAndActiveFlagTrue(EMPLOYEE_ID))
                .thenReturn(Optional.of(stemOptVisa));

        var result = service.getStemOptProgress(EMPLOYEE_ID);

        assertThat(result.currentStep()).isNull();
        assertThat(result.nextStep()).isEqualTo(StemOptStep.I_983);
        assertThat(result.canDownloadI983()).isTrue();
    }

    @Test
    void getStemOptProgress_throwsWhenEmployeeNotFound() {
        when(employeeRepository.existsById(EMPLOYEE_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.getStemOptProgress(EMPLOYEE_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getStemOptProgress_currentStepIsLastUploadedStep() {
        PersonalDocument i983Doc = buildDoc(DocumentType.I_983);
        PersonalDocument i20Doc = buildDoc(DocumentType.I_20);
        when(employeeRepository.existsById(EMPLOYEE_ID)).thenReturn(true);
        when(personalDocumentRepository.findByEmployeeIdAndApplicationTypeOrderByCreatedAtDesc(
                EMPLOYEE_ID, STEM_OPT_TYPE)).thenReturn(List.of(i983Doc, i20Doc));
        when(visaStatusRepository.findByEmployeeIdAndActiveFlagTrue(EMPLOYEE_ID))
                .thenReturn(Optional.of(stemOptVisa));

        var result = service.getStemOptProgress(EMPLOYEE_ID);

        assertThat(result.currentStep()).isEqualTo(StemOptStep.I_20);
        assertThat(result.nextStep()).isEqualTo(StemOptStep.OPT_RECEIPT);
    }

    // ── uploadStemOptDocument ────────────────────────────────────────────────

    @Test
    void uploadStemOptDocument_i983_uploadsWithoutPublishingWorkflowEvent() {
        MockMultipartFile file = new MockMultipartFile("file", "i983.pdf", "application/pdf", new byte[10]);
        when(employeeRepository.findByUserId(USER_ID)).thenReturn(Optional.of(employee));
        when(visaStatusRepository.findByEmployeeIdAndActiveFlagTrue(EMPLOYEE_ID))
                .thenReturn(Optional.of(stemOptVisa));
        when(fileStorageService.upload(any(), anyString())).thenReturn("visa/stem-opt/10/uuid-i983.pdf");
        when(personalDocumentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(visaStatusRepository.save(stemOptVisa)).thenReturn(stemOptVisa);

        service.uploadStemOptDocument(StemOptStep.I_983, file, null, null);

        verify(employeeEventPublisher, never()).publishVisaWorkflowEvent(any());
        verify(employeeEventPublisher).publishEmailEvent(any(EmailEvent.class));
    }

    @Test
    void uploadStemOptDocument_i20_publishesWorkflowEvent() {
        MockMultipartFile file = new MockMultipartFile("file", "i20.pdf", "application/pdf", new byte[10]);
        when(employeeRepository.findByUserId(USER_ID)).thenReturn(Optional.of(employee));
        when(visaStatusRepository.findByEmployeeIdAndActiveFlagTrue(EMPLOYEE_ID))
                .thenReturn(Optional.of(stemOptVisa));
        when(personalDocumentRepository.existsByEmployeeIdAndApplicationTypeAndDocumentType(
                EMPLOYEE_ID, STEM_OPT_TYPE, DocumentType.I_983)).thenReturn(true);
        when(fileStorageService.upload(any(), anyString())).thenReturn("visa/stem-opt/10/uuid-i20.pdf");
        when(personalDocumentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(visaStatusRepository.save(stemOptVisa)).thenReturn(stemOptVisa);

        service.uploadStemOptDocument(StemOptStep.I_20, file, null, null);

        ArgumentCaptor<VisaWorkflowEvent> captor = ArgumentCaptor.forClass(VisaWorkflowEvent.class);
        verify(employeeEventPublisher).publishVisaWorkflowEvent(captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo("I20_UPLOADED");
    }

    @Test
    void uploadStemOptDocument_i20_createsWorkflowAndSyncsDocumentMetadata() {
        MockMultipartFile file = new MockMultipartFile("file", "i20.pdf", "application/pdf", new byte[10]);
        String s3Key = "visa/stem-opt/10/uuid-i20.pdf";

        when(employeeRepository.findByUserId(USER_ID)).thenReturn(Optional.of(employee));
        when(visaStatusRepository.findByEmployeeIdAndActiveFlagTrue(EMPLOYEE_ID))
                .thenReturn(Optional.of(stemOptVisa));
        when(personalDocumentRepository.existsByEmployeeIdAndApplicationTypeAndDocumentType(
                EMPLOYEE_ID, STEM_OPT_TYPE, DocumentType.I_983)).thenReturn(true);
        when(applicationServiceClient.getApplicationsByEmployeeIdAndType(USER_ID, "OPT_STEM"))
                .thenReturn(new ApplicationServiceDataResponse<>("ok", List.of()));
        when(applicationServiceClient.createApplication(any()))
                .thenReturn(new ApplicationServiceDataResponse<>("created",
                        new ApplicationWorkflowResponse(
                                77, USER_ID, null, null, "Open", "I983_DOWNLOADED", null, "OPT_STEM")));
        when(fileStorageService.upload(any(), anyString())).thenReturn(s3Key);
        when(personalDocumentRepository.save(any())).thenAnswer(inv -> {
            PersonalDocument doc = inv.getArgument(0);
            doc.setId("doc-i20");
            return doc;
        });
        when(visaStatusRepository.save(stemOptVisa)).thenReturn(stemOptVisa);

        service.uploadStemOptDocument(StemOptStep.I_20, file, null, null);

        ArgumentCaptor<ApplicationDocumentMetadataRequest> metadataCaptor =
                ArgumentCaptor.forClass(ApplicationDocumentMetadataRequest.class);
        verify(applicationServiceClient).createDocumentMetadata(metadataCaptor.capture());
        ApplicationDocumentMetadataRequest metadata = metadataCaptor.getValue();
        assertThat(metadata.getApplicationId()).isEqualTo(77);
        assertThat(metadata.getType()).isEqualTo("I_20");
        assertThat(metadata.getPath()).isEqualTo(s3Key);
        assertThat(metadata.getSourceDocumentId()).isEqualTo("doc-i20");
        assertThat(metadata.getTitle()).isEqualTo("I-20");
    }

    @Test
    void uploadStemOptDocument_optEad_updatesVisaDates() {
        MockMultipartFile file = new MockMultipartFile("file", "ead.pdf", "application/pdf", new byte[10]);
        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 1);

        when(employeeRepository.findByUserId(USER_ID)).thenReturn(Optional.of(employee));
        when(visaStatusRepository.findByEmployeeIdAndActiveFlagTrue(EMPLOYEE_ID))
                .thenReturn(Optional.of(stemOptVisa));
        when(personalDocumentRepository.existsByEmployeeIdAndApplicationTypeAndDocumentType(
                EMPLOYEE_ID, STEM_OPT_TYPE, DocumentType.OPT_RECEIPT)).thenReturn(true);
        when(fileStorageService.upload(any(), anyString())).thenReturn("visa/stem-opt/10/uuid-ead.pdf");
        when(personalDocumentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(visaStatusRepository.save(stemOptVisa)).thenReturn(stemOptVisa);

        service.uploadStemOptDocument(StemOptStep.OPT_EAD, file, start, end);

        assertThat(stemOptVisa.getStartDate()).isEqualTo(start);
        assertThat(stemOptVisa.getEndDate()).isEqualTo(end);
    }

    @Test
    void uploadStemOptDocument_throwsWhenNotStemOptVisa() {
        MockMultipartFile file = new MockMultipartFile("file", "f.pdf", "application/pdf", new byte[10]);
        VisaStatus h1bVisa = buildVisa(VisaType.H1B);
        when(employeeRepository.findByUserId(USER_ID)).thenReturn(Optional.of(employee));
        when(visaStatusRepository.findByEmployeeIdAndActiveFlagTrue(EMPLOYEE_ID))
                .thenReturn(Optional.of(h1bVisa));

        assertThatThrownBy(() -> service.uploadStemOptDocument(StemOptStep.I_983, file, null, null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("F1 OPT STEM");
    }

    @Test
    void uploadStemOptDocument_throwsWhenPrevStepNotUploaded() {
        MockMultipartFile file = new MockMultipartFile("file", "f.pdf", "application/pdf", new byte[10]);
        when(employeeRepository.findByUserId(USER_ID)).thenReturn(Optional.of(employee));
        when(visaStatusRepository.findByEmployeeIdAndActiveFlagTrue(EMPLOYEE_ID))
                .thenReturn(Optional.of(stemOptVisa));
        when(personalDocumentRepository.existsByEmployeeIdAndApplicationTypeAndDocumentType(
                EMPLOYEE_ID, STEM_OPT_TYPE, DocumentType.I_983)).thenReturn(false);

        assertThatThrownBy(() -> service.uploadStemOptDocument(StemOptStep.I_20, file, null, null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("I_983");
    }

    // ── reviewStemOptApplication ─────────────────────────────────────────────

    @Test
    void reviewStemOptApplication_approvedPublishesEvents() {
        setAuth(USER_ID, "HR");
        when(visaStatusRepository.findById(VISA_ID)).thenReturn(Optional.of(stemOptVisa));
        when(visaStatusRepository.save(stemOptVisa)).thenReturn(stemOptVisa);
        when(employeeRepository.findById(EMPLOYEE_ID)).thenReturn(Optional.of(employee));

        service.reviewStemOptApplication(VISA_ID,
                new ReviewApplicationRequest(ApplicationStatus.APPROVED, "All good"));

        ArgumentCaptor<VisaWorkflowEvent> wfCaptor = ArgumentCaptor.forClass(VisaWorkflowEvent.class);
        verify(employeeEventPublisher).publishVisaWorkflowEvent(wfCaptor.capture());
        assertThat(wfCaptor.getValue().eventType()).isEqualTo("STEM_OPT_APPROVED");

        ArgumentCaptor<EmailEvent> emailCaptor = ArgumentCaptor.forClass(EmailEvent.class);
        verify(employeeEventPublisher).publishEmailEvent(emailCaptor.capture());
        assertThat(emailCaptor.getValue().subject()).isEqualTo("Your OPT STEM application has been approved");
    }

    @Test
    void reviewStemOptApplication_rejectedDeactivatesVisa() {
        setAuth(USER_ID, "HR");
        when(visaStatusRepository.findById(VISA_ID)).thenReturn(Optional.of(stemOptVisa));
        when(visaStatusRepository.save(stemOptVisa)).thenReturn(stemOptVisa);
        when(employeeRepository.findById(EMPLOYEE_ID)).thenReturn(Optional.of(employee));

        service.reviewStemOptApplication(VISA_ID,
                new ReviewApplicationRequest(ApplicationStatus.REJECTED, "Missing docs"));

        assertThat(stemOptVisa.getActiveFlag()).isFalse();
    }

    @Test
    void reviewStemOptApplication_throwsWhenVisaNotFound() {
        when(visaStatusRepository.findById(VISA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reviewStemOptApplication(VISA_ID,
                new ReviewApplicationRequest(ApplicationStatus.APPROVED, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void reviewStemOptApplication_throwsForInvalidStatus() {
        when(visaStatusRepository.findById(VISA_ID)).thenReturn(Optional.of(stemOptVisa));

        assertThatThrownBy(() -> service.reviewStemOptApplication(VISA_ID,
                new ReviewApplicationRequest(ApplicationStatus.PENDING, null)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("APPROVED or REJECTED");
    }

    // ── listAllVisaStatuses ───────────────────────────────────────────────────

    @Test
    void listAllVisaStatuses_includesStemOptCurrentStep() {
        when(visaStatusRepository.findAllByActiveFlagTrue()).thenReturn(List.of(stemOptVisa));
        when(employeeRepository.findById(EMPLOYEE_ID)).thenReturn(Optional.of(employee));
        when(personalDocumentRepository.findByEmployeeIdAndApplicationTypeOrderByCreatedAtDesc(
                EMPLOYEE_ID, STEM_OPT_TYPE)).thenReturn(List.of());

        var result = service.listAllVisaStatuses();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).hasActiveStemOptApp()).isTrue();
        assertThat(result.get(0).currentStep()).isNull();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Employee buildEmployee() {
        Employee e = new Employee();
        e.setId(EMPLOYEE_ID);
        e.setUserId(USER_ID);
        e.setFirstName("Alice");
        e.setLastName("Smith");
        e.setEmail("alice@example.com");
        e.setCitizenshipStatus(CitizenshipStatus.NON_RESIDENT);
        return e;
    }

    private VisaStatus buildVisa(VisaType type) {
        VisaStatus vs = new VisaStatus();
        vs.setId(VISA_ID);
        vs.setEmployeeId(EMPLOYEE_ID);
        vs.setVisaType(type);
        vs.setActiveFlag(true);
        return vs;
    }

    private PersonalDocument buildDoc(DocumentType type) {
        PersonalDocument doc = new PersonalDocument();
        doc.setEmployeeId(EMPLOYEE_ID);
        doc.setDocumentType(type);
        doc.setApplicationType(STEM_OPT_TYPE);
        doc.setS3Key("some/key");
        doc.setTitle(type.name());
        return doc;
    }

    private void setAuth(Long userId, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        userId.toString(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }
}
