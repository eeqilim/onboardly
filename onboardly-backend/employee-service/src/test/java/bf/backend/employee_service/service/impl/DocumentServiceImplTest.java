package bf.backend.employee_service.service.impl;

import bf.backend.employee_service.dto.request.DocumentUploadMetadata;
import bf.backend.employee_service.dto.request.ApplicationDocumentMetadataRequest;
import bf.backend.employee_service.entity.*;
import bf.backend.employee_service.exception.ResourceNotFoundException;
import bf.backend.employee_service.exception.UnauthorizedActionException;
import bf.backend.employee_service.exception.ValidationException;
import bf.backend.employee_service.repository.EmployeeRepository;
import bf.backend.employee_service.repository.PersonalDocumentRepository;
import bf.backend.employee_service.service.FileStorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import bf.backend.employee_service.client.ApplicationServiceClient;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceImplTest {

    private static final Long USER_ID = 1L;
    private static final String EMPLOYEE_ID = "10";
    private static final String DOC_ID = "50";

    @Mock private EmployeeRepository employeeRepository;
    @Mock private PersonalDocumentRepository personalDocumentRepository;
    @Mock private FileStorageService fileStorageService;
    @Mock private ApplicationServiceClient applicationServiceClient;

    @InjectMocks
    private DocumentServiceImpl service;

    private Employee employee;
    private PersonalDocument document;

    @BeforeEach
    void setUp() {
        setAuth(USER_ID, "EMPLOYEE");
        employee = buildEmployee();
        document = buildDocument(employee);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    // ── uploadDocument ───────────────────────────────────────────────────────

    @Test
    void uploadDocument_savesAndReturnsResponse() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.pdf", "application/pdf", new byte[100]);
        var meta = new DocumentUploadMetadata(1, "Resume", "Employee upload note", DocumentType.OTHER, null);
        when(employeeRepository.findByUserId(USER_ID)).thenReturn(Optional.of(employee));
        when(fileStorageService.upload(any(), anyString())).thenReturn("documents/10/uuid-resume.pdf");
        when(personalDocumentRepository.save(any())).thenAnswer(inv -> {
            PersonalDocument d = inv.getArgument(0);
            d.setId(DOC_ID);
            return d;
        });

        var result = service.uploadDocument(file, meta);

        assertThat(result.id()).isEqualTo(DOC_ID);
        assertThat(result.comment()).isEqualTo("Employee upload note");
        verify(fileStorageService).upload(file, "documents/10");

        ArgumentCaptor<ApplicationDocumentMetadataRequest> captor =
                ArgumentCaptor.forClass(ApplicationDocumentMetadataRequest.class);
        verify(applicationServiceClient).createDocumentMetadata(captor.capture());
        assertThat(captor.getValue().getDescription()).isNull();
        assertThat(captor.getValue().getSourceDocumentId()).isEqualTo(DOC_ID);
    }

    @Test
    void uploadDocument_throwsWhenFileTooLarge() {
        byte[] bigData = new byte[(int) (10L * 1024 * 1024 + 1)];
        MockMultipartFile file = new MockMultipartFile("file", "big.pdf", "application/pdf", bigData);
        var meta = new DocumentUploadMetadata(1,"Big", null, DocumentType.OTHER, null);

        assertThatThrownBy(() -> service.uploadDocument(file, meta))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("10 MB limit");
    }

    @Test
    void uploadDocument_throwsWhenUnsupportedContentType() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "script.js", "application/javascript", new byte[100]);
        var meta = new DocumentUploadMetadata(1,"Script", null, DocumentType.OTHER, null);

        assertThatThrownBy(() -> service.uploadDocument(file, meta))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Unsupported file type");
    }

    // ── getDocumentById ──────────────────────────────────────────────────────

    @Test
    void getDocumentById_returnsDocumentForOwner() {
        when(personalDocumentRepository.findById(DOC_ID)).thenReturn(Optional.of(document));
        when(employeeRepository.findByUserId(USER_ID)).thenReturn(Optional.of(employee));

        var result = service.getDocumentById(DOC_ID);

        assertThat(result.id()).isEqualTo(DOC_ID);
    }

    @Test
    void getDocumentById_hrCanAccessAnyDocument() {
        setAuth(USER_ID, "HR");
        when(personalDocumentRepository.findById(DOC_ID)).thenReturn(Optional.of(document));

        var result = service.getDocumentById(DOC_ID);

        assertThat(result.id()).isEqualTo(DOC_ID);
        verify(employeeRepository, never()).findByUserId(anyLong());
    }

    @Test
    void getDocumentById_throwsWhenNonOwnerNonHrAccesses() {
        Employee other = buildOtherEmployee();
        when(personalDocumentRepository.findById(DOC_ID)).thenReturn(Optional.of(document));
        when(employeeRepository.findByUserId(USER_ID)).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.getDocumentById(DOC_ID))
                .isInstanceOf(UnauthorizedActionException.class);
    }

    @Test
    void getDocumentById_throwsWhenDocumentNotFound() {
        when(personalDocumentRepository.findById(DOC_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDocumentById(DOC_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getMyDocuments ───────────────────────────────────────────────────────

    @Test
    void getMyDocuments_returnsOwnDocuments() {
        when(employeeRepository.findByUserId(USER_ID)).thenReturn(Optional.of(employee));
        when(personalDocumentRepository.findByEmployeeIdOrderByCreatedAtDesc(EMPLOYEE_ID))
                .thenReturn(List.of(document));

        var result = service.getMyDocuments();

        assertThat(result).hasSize(1);
    }

    // ── getDocumentsByEmployeeId ─────────────────────────────────────────────

    @Test
    void getDocumentsByEmployeeId_hrCanAccessAnyEmployee() {
        setAuth(USER_ID, "HR");
        when(personalDocumentRepository.findByEmployeeIdOrderByCreatedAtDesc(EMPLOYEE_ID))
                .thenReturn(List.of(document));

        var result = service.getDocumentsByEmployeeId(EMPLOYEE_ID);

        assertThat(result).hasSize(1);
        verify(employeeRepository, never()).findByUserId(anyLong());
    }

    @Test
    void getDocumentsByEmployeeId_throwsWhenNonOwnerNonHrRequests() {
        Employee other = buildOtherEmployee();
        when(employeeRepository.findByUserId(USER_ID)).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.getDocumentsByEmployeeId(EMPLOYEE_ID))
                .isInstanceOf(UnauthorizedActionException.class);
    }

    // ── downloadDocument ─────────────────────────────────────────────────────

    @Test
    void downloadDocument_returnsBytes() {
        byte[] content = {1, 2, 3};
        when(personalDocumentRepository.findById(DOC_ID)).thenReturn(Optional.of(document));
        when(employeeRepository.findByUserId(USER_ID)).thenReturn(Optional.of(employee));
        when(fileStorageService.download(document.getS3Key())).thenReturn(content);

        byte[] result = service.downloadDocument(DOC_ID);

        assertThat(result).isEqualTo(content);
    }

    // ── getPresignedUrl ──────────────────────────────────────────────────────

    @Test
    void getPresignedUrl_returnsUrl() {
        when(personalDocumentRepository.findById(DOC_ID)).thenReturn(Optional.of(document));
        when(employeeRepository.findByUserId(USER_ID)).thenReturn(Optional.of(employee));
        when(fileStorageService.generatePresignedDownloadUrl(anyString(), any(Duration.class)))
                .thenReturn("https://s3.amazonaws.com/presigned");

        String url = service.getPresignedUrl(DOC_ID);

        assertThat(url).startsWith("https://");
    }

    // ── addHrComment ─────────────────────────────────────────────────────────

    @Test
    void getTemplatePresignedUrl_returnsUrlForTemplateKey() {
        when(fileStorageService.generatePresignedDownloadUrl(eq("templates/onboarding/fw4.pdf"), any(Duration.class)))
                .thenReturn("https://s3.amazonaws.com/template-presigned");

        String url = service.getTemplatePresignedUrl("/templates/onboarding/fw4.pdf");

        assertThat(url).startsWith("https://");
    }

    @Test
    void getTemplatePresignedUrl_rejectsNonTemplateKey() {
        assertThatThrownBy(() -> service.getTemplatePresignedUrl("documents/10/file.pdf"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid template key");
    }

    @Test
    void addHrComment_updatesComment() {
        when(personalDocumentRepository.findById(DOC_ID)).thenReturn(Optional.of(document));
        when(personalDocumentRepository.save(document)).thenReturn(document);

        service.addHrComment(DOC_ID, "Needs review");

        assertThat(document.getComment()).isEqualTo("Needs review");
    }

    @Test
    void addHrComment_throwsWhenDocumentNotFound() {
        when(personalDocumentRepository.findById(DOC_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addHrComment(DOC_ID, "comment"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── deleteDocument ────────────────────────────────────────────────────────

    @Test
    void deleteDocument_deletesFromS3AndRepository() {
        when(personalDocumentRepository.findById(DOC_ID)).thenReturn(Optional.of(document));
        when(employeeRepository.findByUserId(USER_ID)).thenReturn(Optional.of(employee));

        service.deleteDocument(DOC_ID);

        verify(fileStorageService).delete(document.getS3Key());
        verify(personalDocumentRepository).delete(document);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Employee buildEmployee() {
        Employee e = new Employee();
        e.setId(EMPLOYEE_ID);
        e.setUserId(USER_ID);
        e.setFirstName("Alice");
        e.setLastName("Smith");
        e.setEmail("alice@example.com");
        e.setCitizenshipStatus(CitizenshipStatus.CITIZEN);
        return e;
    }

    private Employee buildOtherEmployee() {
        Employee e = new Employee();
        e.setId("99");
        e.setUserId(USER_ID);
        e.setFirstName("Bob");
        e.setLastName("Jones");
        e.setEmail("bob@example.com");
        e.setCitizenshipStatus(CitizenshipStatus.CITIZEN);
        return e;
    }

    private PersonalDocument buildDocument(Employee owner) {
        PersonalDocument doc = new PersonalDocument();
        doc.setId(DOC_ID);
        doc.setEmployeeId(owner.getId());
        doc.setDocumentType(DocumentType.OTHER);
        doc.setS3Key("documents/10/uuid-resume.pdf");
        doc.setTitle("Resume");
        return doc;
    }

    private void setAuth(Long userId, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        userId.toString(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }
}
