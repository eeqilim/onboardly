package bf.backend.employee_service.service.impl;

import bf.backend.employee_service.client.ApplicationServiceClient;
import bf.backend.employee_service.dto.request.ApplicationDocumentMetadataRequest;
import bf.backend.employee_service.dto.request.ApplicationWorkflowRequest;
import bf.backend.employee_service.dto.request.ReviewApplicationRequest;
import bf.backend.employee_service.dto.response.ApplicationServiceDataResponse;
import bf.backend.employee_service.dto.response.ApplicationWorkflowResponse;
import bf.backend.employee_service.dto.response.DocumentResponse;
import bf.backend.employee_service.dto.response.StemOptProgressResponse;
import bf.backend.employee_service.dto.response.VisaSummaryResponse;
import bf.backend.employee_service.dto.response.VisaStatusResponse;
import bf.backend.employee_service.entity.*;
import bf.backend.employee_service.exception.ResourceNotFoundException;
import bf.backend.employee_service.exception.ValidationException;
import bf.backend.employee_service.kafka.event.EmailEvent;
import bf.backend.employee_service.kafka.event.VisaWorkflowEvent;
import bf.backend.employee_service.kafka.producer.EmployeeEventPublisher;
import bf.backend.employee_service.mapper.DocumentMapper;
import bf.backend.employee_service.mapper.VisaStatusMapper;
import bf.backend.employee_service.repository.EmployeeRepository;
import bf.backend.employee_service.repository.PersonalDocumentRepository;
import bf.backend.employee_service.repository.VisaStatusRepository;
import bf.backend.employee_service.service.FileStorageService;
import bf.backend.employee_service.service.VisaStatusService;
import bf.backend.employee_service.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VisaStatusServiceImpl implements VisaStatusService {

    private static final String STEM_OPT_TYPE = "VISA_STEM_OPT";
    private static final String OPT_STEM_APPLICATION_TYPE = "OPT_STEM";

    private final EmployeeRepository employeeRepository;
    private final VisaStatusRepository visaStatusRepository;
    private final PersonalDocumentRepository personalDocumentRepository;
    private final FileStorageService fileStorageService;
    private final EmployeeEventPublisher employeeEventPublisher;
    private final ApplicationServiceClient applicationServiceClient;

    // ── employee queries ──────────────────────────────────────────────────────

    @Override
    public VisaStatusResponse getMyActiveVisa() {
        Employee employee = findEmployeeByCurrentUser();
        return visaStatusRepository.findByEmployeeIdAndActiveFlagTrue(employee.getId())
                .map(VisaStatusMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("No active visa found for current user."));
    }

    @Override
    public List<VisaStatusResponse> getMyVisaHistory() {
        Employee employee = findEmployeeByCurrentUser();
        return visaStatusRepository
                .findByEmployeeIdOrderByLastModificationDateDesc(employee.getId())
                .stream()
                .map(VisaStatusMapper::toResponse)
                .toList();
    }

    @Override
    public StemOptProgressResponse getMyStemOptProgress() {
        return getStemOptProgress(findEmployeeByCurrentUser().getId());
    }

    @Override
    public StemOptProgressResponse getStemOptProgress(String employeeId) {
        if (!employeeRepository.existsById(employeeId)) {
            throw new ResourceNotFoundException("Employee not found: " + employeeId);
        }

        List<PersonalDocument> stemDocs = personalDocumentRepository
                .findByEmployeeIdAndApplicationTypeOrderByCreatedAtDesc(employeeId, STEM_OPT_TYPE);

        Set<DocumentType> uploaded = stemDocs.stream()
                .map(PersonalDocument::getDocumentType)
                .collect(Collectors.toSet());

        StemOptStep currentStep = computeCurrentStep(uploaded);
        StemOptStep nextStep = currentStep == null ? StemOptStep.I_983 : currentStep.next();

        boolean canDownloadI983 = visaStatusRepository
                .findByEmployeeIdAndActiveFlagTrue(employeeId)
                .map(v -> v.getVisaType() == VisaType.F1_OPT_STEM)
                .orElse(false);

        List<DocumentResponse> docResponses = stemDocs.stream()
                .map(DocumentMapper::toResponse)
                .toList();

        return new StemOptProgressResponse(currentStep, nextStep, docResponses, canDownloadI983);
    }

    // ── employee commands ─────────────────────────────────────────────────────

    @Override
    public VisaStatusResponse uploadStemOptDocument(StemOptStep step, MultipartFile file,
                                                    LocalDate eadStartDate, LocalDate eadEndDate) {
        Employee employee = findEmployeeByCurrentUser();

        VisaStatus activeVisa = visaStatusRepository
                .findByEmployeeIdAndActiveFlagTrue(employee.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No active visa found for current user."));

        if (activeVisa.getVisaType() != VisaType.F1_OPT_STEM) {
            throw new ValidationException(
                    "STEM OPT document uploads are only available for F1 OPT STEM visa holders.");
        }

        validateStepOrder(step, employee.getId());
        Integer applicationWorkflowId = ensureOptStemWorkflow(employee);

        String s3Key = fileStorageService.upload(
                file, "visa/stem-opt/" + employee.getId());

        PersonalDocument doc = new PersonalDocument();
        doc.setEmployeeId(employee.getId());
        doc.setDocumentType(DocumentType.valueOf(step.name()));
        doc.setS3Key(s3Key);
        doc.setTitle(step.name() + " document");
        doc.setApplicationType(STEM_OPT_TYPE);
        PersonalDocument savedDocument = personalDocumentRepository.save(doc);

        syncStemOptDocumentMetadata(applicationWorkflowId, step, s3Key, savedDocument);

        if (step == StemOptStep.OPT_EAD && eadStartDate != null && eadEndDate != null) {
            activeVisa.setStartDate(eadStartDate);
            activeVisa.setEndDate(eadEndDate);
        }

        String workflowEventType = toVisaWorkflowEventType(step);
        if (workflowEventType != null) {
            employeeEventPublisher.publishVisaWorkflowEvent(new VisaWorkflowEvent(
                    employee.getUserId(),
                    workflowEventType,
                    s3Key,
                    System.currentTimeMillis(),
                    Map.of("visaStatusId", activeVisa.getId(), "step", step.name())
            ));
        }

        String nextStepName = step.next() != null ? step.next().name() : "AWAITING_HR_REVIEW";
        String employeeName = employee.getFirstName() + " " + employee.getLastName();
        employeeEventPublisher.publishEmailEvent(new EmailEvent(
                employee.getEmail(),
                "OPT STEM: " + step.name() + " received — next step: " + nextStepName,
                "Dear " + employeeName + ",\n\nWe have received your " + step.name() + " document.\n"
                        + "Your next step is: " + nextStepName + ".\n\nPlease log in to continue."
        ));

        return VisaStatusMapper.toResponse(visaStatusRepository.save(activeVisa));
    }

    // ── HR queries ────────────────────────────────────────────────────────────

    @Override
    public List<VisaSummaryResponse> listAllVisaStatuses() {
        return visaStatusRepository.findAllByActiveFlagTrue().stream()
                .map(vs -> {
                    Employee emp = employeeRepository.findById(vs.getEmployeeId())
                            .orElse(null);
                    if (emp == null) return null;

                    boolean isStemOpt = vs.getVisaType() == VisaType.F1_OPT_STEM;

                    StemOptStep currentStep = null;
                    if (isStemOpt) {
                        List<PersonalDocument> stemDocs = personalDocumentRepository
                                .findByEmployeeIdAndApplicationTypeOrderByCreatedAtDesc(
                                        emp.getId(), STEM_OPT_TYPE);
                        Set<DocumentType> uploaded = stemDocs.stream()
                                .map(PersonalDocument::getDocumentType)
                                .collect(Collectors.toSet());
                        currentStep = computeCurrentStep(uploaded);
                    }

                    long daysLeft = vs.getEndDate() != null
                            ? ChronoUnit.DAYS.between(LocalDate.now(), vs.getEndDate())
                            : 0L;

                    return new VisaSummaryResponse(
                            vs.getId(),
                            emp.getId(),
                            emp.getFirstName(),
                            emp.getLastName(),
                            vs.getVisaType(),
                            vs.getVisaTypeOther(),
                            vs.getEndDate(),
                            daysLeft,
                            isStemOpt,
                            currentStep
                    );
                })
                .filter(Objects::nonNull)
                .toList();
    }

    // ── HR commands ───────────────────────────────────────────────────────────

    @Override
    public VisaStatusResponse reviewStemOptApplication(String visaStatusId, ReviewApplicationRequest req) {
        VisaStatus visa = visaStatusRepository.findById(visaStatusId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Visa status not found: " + visaStatusId));

        if (req.status() != ApplicationStatus.APPROVED && req.status() != ApplicationStatus.REJECTED) {
            throw new ValidationException("Review status must be APPROVED or REJECTED.");
        }

        if (req.status() == ApplicationStatus.REJECTED) {
            visa.setActiveFlag(false);
        }

        VisaStatus saved = visaStatusRepository.save(visa);

        Employee emp = employeeRepository.findById(visa.getEmployeeId())
                .orElse(null);
        if (emp != null) {
            boolean approved = req.status() == ApplicationStatus.APPROVED;
            String feedback = Objects.requireNonNullElse(req.hrFeedback(), "");

            employeeEventPublisher.publishVisaWorkflowEvent(new VisaWorkflowEvent(
                    emp.getUserId(),
                    approved ? "STEM_OPT_APPROVED" : "STEM_OPT_REJECTED",
                    null,
                    System.currentTimeMillis(),
                    Map.of("visaStatusId", visaStatusId, "hrFeedback", feedback)
            ));

            String empName = emp.getFirstName() + " " + emp.getLastName();
            String subject = approved
                    ? "Your OPT STEM application has been approved"
                    : "Your OPT STEM application has been rejected";
            String body = approved
                    ? "Dear " + empName + ",\n\nYour OPT STEM application has been approved."
                    : "Dear " + empName + ",\n\nYour OPT STEM application has been rejected."
                            + (feedback.isEmpty() ? "" : "\n\nHR Feedback: " + feedback);
            employeeEventPublisher.publishEmailEvent(new EmailEvent(emp.getEmail(), subject, body));
        }

        return VisaStatusMapper.toResponse(saved);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Employee findEmployeeByCurrentUser() {
        Long userId = SecurityUtils.getCurrentUserId();
        return employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No employee profile found for current user."));
    }

    private void validateStepOrder(StemOptStep step, String employeeId) {
        int idx = step.ordinal();
        if (idx == 0) return;

        StemOptStep prev = StemOptStep.values()[idx - 1];
        boolean prevUploaded = personalDocumentRepository
                .existsByEmployeeIdAndApplicationTypeAndDocumentType(
                        employeeId, STEM_OPT_TYPE, DocumentType.valueOf(prev.name()));
        if (!prevUploaded) {
            throw new ValidationException(
                    "Cannot upload " + step.name() + " before " + prev.name() + " has been submitted.");
        }
    }

    private Integer ensureOptStemWorkflow(Employee employee) {
        if (employee.getUserId() == null) {
            return null;
        }

        Integer workflowId = findOpenApplicationWorkflowId(employee.getUserId(), OPT_STEM_APPLICATION_TYPE);
        if (workflowId == null) {
            workflowId = createApplicationWorkflow(employee.getUserId(), OPT_STEM_APPLICATION_TYPE);
        }

        return workflowId;
    }

    private Integer findOpenApplicationWorkflowId(Long userId, String applicationType) {
        try {
            ApplicationServiceDataResponse<List<ApplicationWorkflowResponse>> response =
                    applicationServiceClient.getApplicationsByEmployeeIdAndType(userId, applicationType);
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

    private Integer createApplicationWorkflow(Long userId, String applicationType) {
        try {
            ApplicationServiceDataResponse<ApplicationWorkflowResponse> response =
                    applicationServiceClient.createApplication(ApplicationWorkflowRequest.builder()
                            .employeeId(userId)
                            .applicationType(applicationType)
                            .build());
            return response == null || response.data() == null ? null : response.data().id();
        } catch (Exception e) {
            return findOpenApplicationWorkflowId(userId, applicationType);
        }
    }

    private void syncStemOptDocumentMetadata(Integer applicationWorkflowId, StemOptStep step,
                                             String s3Key, PersonalDocument savedDocument) {
        if (applicationWorkflowId == null) {
            return;
        }

        try {
            applicationServiceClient.createDocumentMetadata(
                    ApplicationDocumentMetadataRequest.builder()
                            .applicationId(applicationWorkflowId)
                            .type(step.name())
                            .isRequired(1)
                            .path(s3Key)
                            .sourceDocumentId(savedDocument.getId())
                            .title(titleForStemOptStep(step))
                            .build()
            );
        } catch (Exception e) {
            System.err.println("Failed to sync STEM OPT document metadata to Application Service. applicationId="
                    + applicationWorkflowId + ", step=" + step.name());
        }
    }

    private static String titleForStemOptStep(StemOptStep step) {
        return switch (step) {
            case I_983 -> "I-983 Form";
            case I_20 -> "I-20";
            case OPT_RECEIPT -> "OPT STEM Receipt";
            case OPT_EAD -> "OPT STEM EAD";
        };
    }

    private static StemOptStep computeCurrentStep(Set<DocumentType> uploaded) {
        StemOptStep current = null;
        for (StemOptStep s : StemOptStep.values()) {
            if (uploaded.contains(DocumentType.valueOf(s.name()))) {
                current = s;
            }
        }
        return current;
    }

    private static String toVisaWorkflowEventType(StemOptStep step) {
        return switch (step) {
            case I_20 -> "I20_UPLOADED";
            case OPT_RECEIPT -> "OPT_RECEIPT_UPLOADED";
            case OPT_EAD -> "OPT_EAD_UPLOADED";
            default -> null;
        };
    }
}
