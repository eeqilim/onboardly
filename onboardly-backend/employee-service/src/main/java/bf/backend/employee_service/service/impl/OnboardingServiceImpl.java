package bf.backend.employee_service.service.impl;

import bf.backend.employee_service.client.ApplicationServiceClient;
import bf.backend.employee_service.dto.request.AddCommentRequest;
import bf.backend.employee_service.dto.request.ApplicationWorkflowRequest;
import bf.backend.employee_service.dto.request.OnboardingApplicationRequest;
import bf.backend.employee_service.dto.request.ReviewApplicationRequest;
import bf.backend.employee_service.dto.response.ApplicationServiceDataResponse;
import bf.backend.employee_service.dto.response.ApplicationWorkflowResponse;
import bf.backend.employee_service.dto.response.ApplicationCommentResponse;
import bf.backend.employee_service.dto.response.HrOnboardingProfileResponse;
import bf.backend.employee_service.dto.response.OnboardingApplicationResponse;
import bf.backend.employee_service.entity.*;
import bf.backend.employee_service.exception.ResourceNotFoundException;
import bf.backend.employee_service.exception.ValidationException;
import bf.backend.employee_service.kafka.event.EmailEvent;
import bf.backend.employee_service.kafka.producer.EmployeeEventPublisher;
import bf.backend.employee_service.mapper.*;
import bf.backend.employee_service.repository.*;
import bf.backend.employee_service.service.FileStorageService;
import bf.backend.employee_service.service.OnboardingService;
import bf.backend.employee_service.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OnboardingServiceImpl implements OnboardingService {

    private final EmployeeRepository employeeRepository;
    private final OnboardingApplicationRepository onboardingApplicationRepository;
    private final PersonalDocumentRepository personalDocumentRepository;
    private final VisaStatusRepository visaStatusRepository;
    private final EmployeeEventPublisher employeeEventPublisher;
    private final FileStorageService fileStorageService;
    private final ApplicationServiceClient applicationServiceClient;

    // ── queries ──────────────────────────────────────────────────────────────

    @Override
    public OnboardingApplicationResponse getMyApplication() {
        Employee employee = findEmployeeByCurrentUser();
        return onboardingApplicationRepository
                .findFirstByEmployeeIdOrderByCreatedAtDesc(employee.getId())
                .map(OnboardingMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No onboarding application found. Call POST /employee/onboarding/start first."));
    }

    @Override
    public List<OnboardingApplicationResponse> listPendingApplications() {
        return onboardingApplicationRepository.findByStatus(ApplicationStatus.PENDING)
                .stream()
                .map(OnboardingMapper::toResponse)
                .toList();
    }

    // ── commands ─────────────────────────────────────────────────────────────

    @Override
    public OnboardingApplicationResponse startApplication() {
        Long userId = SecurityUtils.getCurrentUserId();

        String userEmail = SecurityUtils.getCurrentUserEmail();

        Employee employee = employeeRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Employee e = new Employee();
                    e.setUserId(userId);
                    e.setFirstName("PENDING");
                    e.setLastName("ONBOARDING");
                    e.setEmail(userEmail != null ? userEmail : "pending_" + userId + "@onboarding.local");
                    e.setCitizenshipStatus(CitizenshipStatus.CITIZEN);
                    return employeeRepository.save(e);
                });

        if (userEmail != null && (employee.getEmail() == null || employee.getEmail().endsWith("@onboarding.local"))) {
            employee.setEmail(userEmail);
            employeeRepository.save(employee);
        }

        OnboardingApplication application = onboardingApplicationRepository
                .findFirstByEmployeeIdOrderByCreatedAtDesc(employee.getId())
                .orElseGet(() -> onboardingApplicationRepository.save(
                        OnboardingMapper.newApplication(employee)));

        ensureApplicationWorkflow(employee, application);

        return OnboardingMapper.toResponse(application);
    }

    @Override
    public OnboardingApplicationResponse submitApplication(OnboardingApplicationRequest req) {
        Employee employee = findEmployeeByCurrentUser();
        OnboardingApplication application = onboardingApplicationRepository
                .findFirstByEmployeeIdOrderByCreatedAtDesc(employee.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No onboarding application found. Call POST /employee/onboarding/start first."));

        if (application.getStatus() == ApplicationStatus.PENDING) {
            throw new ValidationException("Application is already pending review.");
        }
        if (application.getStatus() == ApplicationStatus.APPROVED) {
            throw new ValidationException("Application has already been approved.");
        }

        EmployeeMapper.applyOnboardingRequest(employee, req);

        employee.getAddresses().clear();
        Address address = AddressMapper.toEntity(req.address());
        employee.addAddress(address);

        if (req.referenceContact() == null) {
            throw new ValidationException("Exactly one reference contact is required.");
        }
        employee.getContacts().clear();

        Contact ref = ContactMapper.toEntity(req.referenceContact());
        ref.setType(ContactType.REFERENCE);
        employee.addContact(ref);

        for (var emReq : req.emergencyContacts()) {
            Contact em = ContactMapper.toEntity(emReq);
            em.setType(ContactType.EMERGENCY);
            employee.addContact(em);
        }

        if (employee.getCitizenshipStatus() == CitizenshipStatus.NON_RESIDENT) {
            if (req.visaInfo() == null) {
                throw new ValidationException("Visa information is required for non-residents.");
            }
            if (req.visaInfo().visaType() == VisaType.OTHER
                    && (req.visaInfo().visaTypeOther() == null
                        || req.visaInfo().visaTypeOther().isBlank())) {
                throw new ValidationException(
                        "visaTypeOther must be provided when visa type is OTHER.");
            }
            List<VisaStatus> existing = visaStatusRepository.findByEmployeeId(employee.getId());
            existing.forEach(v -> v.setActiveFlag(false));
            visaStatusRepository.saveAll(existing);
            visaStatusRepository.save(VisaStatusMapper.toEntity(req.visaInfo(), employee));
        } else {
            List<VisaStatus> existing = visaStatusRepository.findByEmployeeId(employee.getId());
            existing.forEach(v -> v.setActiveFlag(false));
            visaStatusRepository.saveAll(existing);
        }

        validateRequiredDocuments(employee, req);

        application.setStatus(ApplicationStatus.PENDING);
        application.setSubmittedAt(LocalDateTime.now());

        employeeRepository.save(employee);
        return OnboardingMapper.toResponse(onboardingApplicationRepository.save(application));
    }

    @Override
    public OnboardingApplicationResponse reviewApplication(String applicationId,
                                                           ReviewApplicationRequest req) {
        OnboardingApplication application = onboardingApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Onboarding application not found: " + applicationId));

        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw new ValidationException(
                    "Only PENDING applications can be reviewed. Current status: "
                            + application.getStatus());
        }

        if (req.status() != ApplicationStatus.APPROVED
                && req.status() != ApplicationStatus.REJECTED) {
            throw new ValidationException("Review status must be APPROVED or REJECTED.");
        }

        Long reviewerId = SecurityUtils.getCurrentUserId();
        application.setStatus(req.status());
        application.setReviewedAt(LocalDateTime.now());
        application.setReviewedBy(reviewerId);
        application.setHrFeedback(req.hrFeedback());

        OnboardingApplication saved = onboardingApplicationRepository.save(application);
        emitNotification(saved);
        return OnboardingMapper.toResponse(saved);
    }

    @Override
    public ApplicationCommentResponse addComment(String applicationId, AddCommentRequest req) {
        OnboardingApplication application = onboardingApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Onboarding application not found: " + applicationId));

        ApplicationComment comment = new ApplicationComment(
                UUID.randomUUID().toString(),
                SecurityUtils.getCurrentUserId(),
                LocalDateTime.now(),
                req.content()
        );
        application.getComments().add(comment);
        onboardingApplicationRepository.save(application);
        return new ApplicationCommentResponse(
                comment.getId(),
                comment.getAuthorId(),
                comment.getCreatedAt(),
                comment.getContent()
        );
    }

    @Override
    public HrOnboardingProfileResponse getHrOnboardingProfile(String employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found: " + employeeId));

        VisaStatus activeVisa = visaStatusRepository
                .findByEmployeeIdAndActiveFlagTrue(employee.getId())
                .orElse(null);

        List<PersonalDocument> documents = personalDocumentRepository
                .findByEmployeeIdOrderByCreatedAtDesc(employee.getId());

        OnboardingApplication application = onboardingApplicationRepository
                .findFirstByEmployeeIdOrderByCreatedAtDesc(employee.getId())
                .orElse(null);

        String resolvedAvatarUrl = resolveAvatarUrl(employee.getAvatarUrl());
        return EmployeeMapper.toHrOnboardingProfile(employee, resolvedAvatarUrl, activeVisa, documents, application);
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private String resolveAvatarUrl(String avatarUrl) {
        if (avatarUrl != null && !avatarUrl.startsWith("/") && !avatarUrl.startsWith("http")) {
            return fileStorageService.generatePresignedDownloadUrl(avatarUrl, Duration.ofHours(1));
        }
        return avatarUrl;
    }

    private Employee findEmployeeByCurrentUser() {
        Long userId = SecurityUtils.getCurrentUserId();
        return employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No employee profile found for current user. "
                                + "Call POST /employee/onboarding/start first."));
    }

    private void ensureApplicationWorkflow(Employee employee, OnboardingApplication application) {
        if (application.getApplicationWorkflowId() != null || employee.getUserId() == null) {
            return;
        }

        Integer workflowId = findOpenOnboardingWorkflowId(employee.getUserId());
        if (workflowId == null) {
            workflowId = createOnboardingWorkflow(employee.getUserId());
        }

        if (workflowId != null) {
            application.setApplicationWorkflowId(workflowId);
            onboardingApplicationRepository.save(application);
        }
    }

    private Integer findOpenOnboardingWorkflowId(Long userId) {
        try {
            ApplicationServiceDataResponse<List<ApplicationWorkflowResponse>> response =
                    applicationServiceClient.getApplicationsByEmployeeIdAndType(userId, "ONBOARDING");
            if (response == null || response.data() == null) {
                return null;
            }

            return response.data()
                    .stream()
                    .filter(workflow -> workflow.id() != null)
                    .filter(workflow -> !"Completed".equalsIgnoreCase(workflow.status()))
                    .findFirst()
                    .map(ApplicationWorkflowResponse::id)
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private Integer createOnboardingWorkflow(Long userId) {
        try {
            ApplicationServiceDataResponse<ApplicationWorkflowResponse> response =
                    applicationServiceClient.createApplication(ApplicationWorkflowRequest.builder()
                            .employeeId(userId)
                            .applicationType("ONBOARDING")
                            .build());
            return response == null || response.data() == null ? null : response.data().id();
        } catch (Exception e) {
            return findOpenOnboardingWorkflowId(userId);
        }
    }

    private void validateRequiredDocuments(Employee employee, OnboardingApplicationRequest req) {
        Set<DocumentType> uploaded = personalDocumentRepository
                .findByEmployeeIdOrderByCreatedAtDesc(employee.getId())
                .stream()
                .map(PersonalDocument::getDocumentType)
                .collect(Collectors.toSet());

        if (req.driverLicense() != null && !req.driverLicense().isBlank()
                && !uploaded.contains(DocumentType.DRIVER_LICENSE)) {
            throw new ValidationException(
                    "A driver's licence document must be uploaded before submitting.");
        }

        if (employee.getCitizenshipStatus() == CitizenshipStatus.NON_RESIDENT
                && !uploaded.contains(DocumentType.WORK_AUTH)) {
            throw new ValidationException(
                    "A work-authorisation document must be uploaded before submitting.");
        }
    }

    private void emitNotification(OnboardingApplication application) {
        Employee employee = employeeRepository.findById(application.getEmployeeId())
                .orElse(null);
        if (employee == null) return;

        boolean approved = application.getStatus() == ApplicationStatus.APPROVED;
        String employeeName = employee.getFirstName() + " " + employee.getLastName();
        String hrFeedback = Objects.requireNonNullElse(application.getHrFeedback(), "");

        String subject = approved
                ? "Your onboarding application has been approved"
                : "Your onboarding application requires updates";
        String body = approved
                ? "Dear " + employeeName + ",\n\nYour onboarding application has been approved. Welcome aboard!"
                : "Dear " + employeeName + ",\n\nYour onboarding application requires updates."
                        + (hrFeedback.isEmpty() ? "" : "\n\nHR Feedback: " + hrFeedback)
                        + "\n\nPlease log in to review your application and resubmit.";

        employeeEventPublisher.publishEmailEvent(new EmailEvent(employee.getEmail(), subject, body));
    }
}
