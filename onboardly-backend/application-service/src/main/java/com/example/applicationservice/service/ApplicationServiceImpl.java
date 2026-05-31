package com.example.applicationservice.service;

import com.example.applicationservice.domain.ApplicationWorkFlow;
import com.example.applicationservice.dto.request.AdvanceWorkflowRequest;
import com.example.applicationservice.dto.request.ApplicationRequest;
import com.example.applicationservice.dto.request.ApplicationReviewRequest;
import com.example.applicationservice.dto.response.ApplicationResponse;
import com.example.applicationservice.exception.ApplicationNotFoundException;
import com.example.applicationservice.exception.DuplicateApplicationException;
import com.example.applicationservice.exception.InvalidApplicationStatusException;
import com.example.applicationservice.repository.ApplicationWorkFlowRepository;
import com.example.applicationservice.domain.DigitalDocument;
import com.example.applicationservice.repository.DigitalDocumentRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationWorkFlowRepository applicationRepository;
    private final DigitalDocumentRepository documentRepository;
    private final EmailNotificationService emailNotificationService;

    @Override
    @Transactional
    public ApplicationResponse createApplication(ApplicationRequest request) {
        applicationRepository.findByEmployeeIdAndApplicationType(
                        request.getEmployeeId(), request.getApplicationType())
                .ifPresent(app -> {
                    if (!app.getStatus().equals("Completed")) {
                        throw new DuplicateApplicationException(
                                "Employee already has an ongoing " + request.getApplicationType() + " application");
                    }
                });

        ApplicationWorkFlow application = ApplicationWorkFlow.builder()
                .employeeId(request.getEmployeeId())
                .applicationType(request.getApplicationType())
                .status("Open")
                .currentStep(initialStepFor(request.getApplicationType()))
                .createDate(LocalDateTime.now())
                .lastModificationDate(LocalDateTime.now())
                .build();

        ApplicationWorkFlow savedApplication = applicationRepository.save(application);

        seedOnboardingDocuments(savedApplication);

        return toApplicationResponse(savedApplication);    }

    @Override
    public ApplicationResponse getApplicationById(Integer id) {
        ApplicationWorkFlow application = applicationRepository.findById(id)
                .orElseThrow(() -> new ApplicationNotFoundException(
                        "Application not found with id: " + id));
        return toApplicationResponse(application);
    }

    @Override
    public List<ApplicationResponse> getApplicationsByEmployeeId(Long employeeId) {
        return applicationRepository.findByEmployeeId(employeeId)
                .stream()
                .map(this::toApplicationResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ApplicationResponse> getApplicationsByEmployeeIdAndType(Long employeeId, String applicationType) {
        return applicationRepository.findAllByEmployeeIdAndApplicationType(employeeId, applicationType)
                .stream()
                .map(this::toApplicationResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ApplicationResponse> getAllApplications() {
        return applicationRepository.findAll()
                .stream()
                .map(this::toApplicationResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ApplicationResponse> getApplicationsByStatus(String status) {
        return applicationRepository.findByStatus(status)
                .stream()
                .map(this::toApplicationResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ApplicationResponse> getApplicationsByType(String applicationType) {
        return applicationRepository.findByApplicationType(applicationType)
                .stream()
                .map(this::toApplicationResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ApplicationResponse reviewApplication(Integer id, ApplicationReviewRequest request) {
        ApplicationWorkFlow application = applicationRepository.findById(id)
                .orElseThrow(() -> new ApplicationNotFoundException(
                        "Application not found with id: " + id));

        // check if application is Open
        if (!application.getStatus().equals("Open")) {
            throw new InvalidApplicationStatusException(
                    "Application status is " + application.getStatus() + ", only Open applications can be reviewed");
        }

        // check if request status is Completed or Rejected
        if (!request.getStatus().equals("Completed") && !request.getStatus().equals("Rejected")) {
            throw new InvalidApplicationStatusException(
                    "Status must be Completed or Rejected, but got: " + request.getStatus());
        }

        application.setStatus(request.getStatus());
        application.setCurrentStep(request.getStatus());
        application.setComment(request.getComment());
        application.setLastModificationDate(LocalDateTime.now());

        ApplicationWorkFlow savedApplication = applicationRepository.save(application);
        emailNotificationService.sendApplicationReviewEmail(savedApplication, request.getEmployeeEmail());

        return toApplicationResponse(savedApplication);
    }

    @Override
    @Transactional
    public ApplicationResponse advanceWorkflow(Integer id, AdvanceWorkflowRequest request) {
        ApplicationWorkFlow application = applicationRepository.findById(id)
                .orElseThrow(() -> new ApplicationNotFoundException(
                        "Application not found with id: " + id));

        if (!application.getStatus().equals("Open")) {
            throw new InvalidApplicationStatusException(
                    "Application status is " + application.getStatus() + ", only Open applications can be advanced");
        }

        application.setCurrentStep(request.getCurrentStep());
        application.setComment(request.getComment());
        application.setLastModificationDate(LocalDateTime.now());

        return toApplicationResponse(applicationRepository.save(application));
    }

    @Override
    @Transactional
    public ApplicationResponse advanceOptStemWorkflowByUserId(Long userId, AdvanceWorkflowRequest request) {
        ApplicationWorkFlow application = applicationRepository
                .findByEmployeeIdAndApplicationType(userId, "OPT_STEM")
                .orElseThrow(() -> new ApplicationNotFoundException(
                        "Open OPT_STEM application not found for userId: " + userId));

        if (!application.getStatus().equals("Open")) {
            throw new InvalidApplicationStatusException(
                    "Application status is " + application.getStatus() + ", only Open applications can be advanced");
        }

        application.setCurrentStep(request.getCurrentStep());
        application.setComment(request.getComment());
        application.setLastModificationDate(LocalDateTime.now());

        return toApplicationResponse(applicationRepository.save(application));
    }

    @Override
    @Transactional
    public ApplicationResponse reviewOptStemWorkflowByUserId(Long userId, String status, String comment) {
        ApplicationWorkFlow application = applicationRepository
                .findByEmployeeIdAndApplicationType(userId, "OPT_STEM")
                .orElseThrow(() -> new ApplicationNotFoundException(
                        "OPT_STEM application not found for userId: " + userId));

        if (!"Completed".equals(status) && !"Rejected".equals(status)) {
            throw new InvalidApplicationStatusException(
                    "Status must be Completed or Rejected, but got: " + status);
        }

        application.setStatus(status);
        application.setCurrentStep(status);
        application.setComment(comment);
        application.setLastModificationDate(LocalDateTime.now());

        return toApplicationResponse(applicationRepository.save(application));
    }

    private ApplicationResponse toApplicationResponse(ApplicationWorkFlow application) {
        return ApplicationResponse.builder()
                .id(application.getId())
                .employeeId(application.getEmployeeId())
                .createDate(application.getCreateDate())
                .lastModificationDate(application.getLastModificationDate())
                .status(application.getStatus())
                .currentStep(application.getCurrentStep())
                .comment(application.getComment())
                .applicationType(application.getApplicationType())
                .documents(List.of())
                .build();
    }
    private void seedOnboardingDocuments(ApplicationWorkFlow application) {
        if (!"ONBOARDING".equalsIgnoreCase(application.getApplicationType())) {
            return;
        }

        List<DigitalDocument> documents = DocumentTemplateCatalog.forApplicationType(application.getApplicationType())
                .stream()
                .map(template -> buildOnboardingDocument(application, template))
                .toList();
        documentRepository.saveAll(documents);
    }

    private DigitalDocument buildOnboardingDocument(ApplicationWorkFlow application,
                                                    DocumentTemplateCatalog.TemplateSpec template) {
        return DigitalDocument.builder()
                .applicationWorkFlow(application)
                .type(template.type())
                .isRequired(template.isRequired())
                .path("")
                .title(template.title())
                .description(template.description())
                .build();
    }

    private String initialStepFor(String applicationType) {
        if ("OPT_STEM".equals(applicationType)) {
            return "I983_DOWNLOADED";
        }
        return "ONBOARDING_SUBMITTED";
    }
}
