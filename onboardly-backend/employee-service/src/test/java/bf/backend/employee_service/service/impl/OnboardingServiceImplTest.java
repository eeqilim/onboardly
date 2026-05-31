package bf.backend.employee_service.service.impl;

import bf.backend.employee_service.dto.request.ReviewApplicationRequest;
import bf.backend.employee_service.entity.*;
import bf.backend.employee_service.exception.ResourceNotFoundException;
import bf.backend.employee_service.exception.ValidationException;
import bf.backend.employee_service.kafka.event.EmailEvent;
import bf.backend.employee_service.kafka.producer.EmployeeEventPublisher;
import bf.backend.employee_service.repository.EmployeeRepository;
import bf.backend.employee_service.repository.OnboardingApplicationRepository;
import bf.backend.employee_service.repository.PersonalDocumentRepository;
import bf.backend.employee_service.repository.VisaStatusRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OnboardingServiceImplTest {

    private static final Long USER_ID = 1L;
    private static final String EMPLOYEE_ID = "10";
    private static final String APP_ID = "100";

    @Mock private EmployeeRepository employeeRepository;
    @Mock private OnboardingApplicationRepository onboardingApplicationRepository;
    @Mock private PersonalDocumentRepository personalDocumentRepository;
    @Mock private VisaStatusRepository visaStatusRepository;
    @Mock private EmployeeEventPublisher employeeEventPublisher;

    @InjectMocks
    private OnboardingServiceImpl service;

    private Employee employee;

    @BeforeEach
    void setUp() {
        setAuth(USER_ID, "EMPLOYEE");
        employee = buildEmployee();
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    // ── getMyApplication ─────────────────────────────────────────────────────

    @Test
    void getMyApplication_returnsExistingApplication() {
        OnboardingApplication app = buildApp(ApplicationStatus.NOT_STARTED);
        when(employeeRepository.findByUserId(USER_ID)).thenReturn(Optional.of(employee));
        when(onboardingApplicationRepository.findFirstByEmployeeIdOrderByCreatedAtDesc(EMPLOYEE_ID))
                .thenReturn(Optional.of(app));

        var result = service.getMyApplication();

        assertThat(result.status()).isEqualTo(ApplicationStatus.NOT_STARTED);
    }

    @Test
    void getMyApplication_throwsWhenNoApplication() {
        when(employeeRepository.findByUserId(USER_ID)).thenReturn(Optional.of(employee));
        when(onboardingApplicationRepository.findFirstByEmployeeIdOrderByCreatedAtDesc(EMPLOYEE_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMyApplication())
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── startApplication ─────────────────────────────────────────────────────

    @Test
    void startApplication_returnsExistingApplicationWhenPresent() {
        OnboardingApplication app = buildApp(ApplicationStatus.NOT_STARTED);
        when(employeeRepository.findByUserId(USER_ID)).thenReturn(Optional.of(employee));
        when(onboardingApplicationRepository.findFirstByEmployeeIdOrderByCreatedAtDesc(EMPLOYEE_ID))
                .thenReturn(Optional.of(app));

        var result = service.startApplication();

        assertThat(result.employeeId()).isEqualTo(EMPLOYEE_ID);
        verify(onboardingApplicationRepository, never()).save(any());
    }

    @Test
    void startApplication_createsEmployeeAndApplicationWhenNoneExist() {
        OnboardingApplication newApp = buildApp(ApplicationStatus.NOT_STARTED);
        when(employeeRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(employeeRepository.save(any(Employee.class))).thenReturn(employee);
        when(onboardingApplicationRepository.findFirstByEmployeeIdOrderByCreatedAtDesc(EMPLOYEE_ID))
                .thenReturn(Optional.empty());
        when(onboardingApplicationRepository.save(any(OnboardingApplication.class))).thenReturn(newApp);

        var result = service.startApplication();

        assertThat(result).isNotNull();
        verify(employeeRepository).save(any(Employee.class));
        verify(onboardingApplicationRepository).save(any(OnboardingApplication.class));
    }

    // ── listPendingApplications ───────────────────────────────────────────────

    @Test
    void listPendingApplications_returnsOnlyPendingApplications() {
        OnboardingApplication pending = buildApp(ApplicationStatus.PENDING);
        when(onboardingApplicationRepository.findByStatus(ApplicationStatus.PENDING))
                .thenReturn(List.of(pending));

        var result = service.listPendingApplications();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo(ApplicationStatus.PENDING);
    }

    // ── reviewApplication ────────────────────────────────────────────────────

    @Test
    void reviewApplication_approvesAndEmitsNotification() {
        setAuth(USER_ID, "HR");
        OnboardingApplication app = buildApp(ApplicationStatus.PENDING);

        when(onboardingApplicationRepository.findById(APP_ID)).thenReturn(Optional.of(app));
        when(onboardingApplicationRepository.save(app)).thenReturn(app);
        when(employeeRepository.findById(EMPLOYEE_ID)).thenReturn(Optional.of(employee));

        var req = new ReviewApplicationRequest(ApplicationStatus.APPROVED, "Looks good");
        service.reviewApplication(APP_ID, req);

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.APPROVED);

        ArgumentCaptor<EmailEvent> captor = ArgumentCaptor.forClass(EmailEvent.class);
        verify(employeeEventPublisher).publishEmailEvent(captor.capture());
        assertThat(captor.getValue().to()).isEqualTo("alice@example.com");
        assertThat(captor.getValue().subject()).isEqualTo("Your onboarding application has been approved");
    }

    @Test
    void reviewApplication_rejectsAndEmitsNotification() {
        setAuth(USER_ID, "HR");
        OnboardingApplication app = buildApp(ApplicationStatus.PENDING);

        when(onboardingApplicationRepository.findById(APP_ID)).thenReturn(Optional.of(app));
        when(onboardingApplicationRepository.save(app)).thenReturn(app);
        when(employeeRepository.findById(EMPLOYEE_ID)).thenReturn(Optional.of(employee));

        var req = new ReviewApplicationRequest(ApplicationStatus.REJECTED, "Missing docs");
        service.reviewApplication(APP_ID, req);

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.REJECTED);

        ArgumentCaptor<EmailEvent> captor = ArgumentCaptor.forClass(EmailEvent.class);
        verify(employeeEventPublisher).publishEmailEvent(captor.capture());
        assertThat(captor.getValue().subject()).isEqualTo("Your onboarding application requires updates");
        assertThat(captor.getValue().body()).contains("Missing docs");
    }

    @Test
    void reviewApplication_throwsWhenApplicationNotFound() {
        when(onboardingApplicationRepository.findById(APP_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reviewApplication(APP_ID,
                new ReviewApplicationRequest(ApplicationStatus.APPROVED, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void reviewApplication_throwsWhenApplicationNotPending() {
        OnboardingApplication app = buildApp(ApplicationStatus.APPROVED);
        when(onboardingApplicationRepository.findById(APP_ID)).thenReturn(Optional.of(app));

        assertThatThrownBy(() -> service.reviewApplication(APP_ID,
                new ReviewApplicationRequest(ApplicationStatus.APPROVED, null)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("PENDING");
    }

    @Test
    void reviewApplication_throwsForInvalidReviewStatus() {
        OnboardingApplication app = buildApp(ApplicationStatus.PENDING);
        when(onboardingApplicationRepository.findById(APP_ID)).thenReturn(Optional.of(app));

        assertThatThrownBy(() -> service.reviewApplication(APP_ID,
                new ReviewApplicationRequest(ApplicationStatus.NOT_STARTED, null)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("APPROVED or REJECTED");
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

    private OnboardingApplication buildApp(ApplicationStatus status) {
        OnboardingApplication app = new OnboardingApplication();
        app.setId(APP_ID);
        app.setEmployeeId(employee.getId());
        app.setStatus(status);
        return app;
    }

    private void setAuth(Long userId, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        userId.toString(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }
}
