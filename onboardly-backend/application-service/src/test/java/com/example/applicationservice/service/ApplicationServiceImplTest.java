package com.example.applicationservice.service;

import com.example.applicationservice.domain.ApplicationWorkFlow;
import com.example.applicationservice.domain.DigitalDocument;
import com.example.applicationservice.dto.request.AdvanceWorkflowRequest;
import com.example.applicationservice.dto.request.ApplicationRequest;
import com.example.applicationservice.dto.request.ApplicationReviewRequest;
import com.example.applicationservice.dto.response.ApplicationResponse;
import com.example.applicationservice.exception.ApplicationNotFoundException;
import com.example.applicationservice.exception.DuplicateApplicationException;
import com.example.applicationservice.exception.InvalidApplicationStatusException;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceImplTest {

    @Mock
    private ApplicationWorkFlowRepository applicationRepository;

    @Mock
    private EmailNotificationService emailNotificationService;

    @Mock
    private DigitalDocumentRepository documentRepository;

    @InjectMocks
    private ApplicationServiceImpl applicationService;

    @Test
    void createApplication_createsOpenApplicationWhenNoOngoingApplicationExists() {
        ApplicationRequest request = ApplicationRequest.builder()
                .employeeId(1L)
                .applicationType("ONBOARDING")
                .build();

        ApplicationWorkFlow savedApplication = application(10, 1L, "Open", "ONBOARDING");

        when(applicationRepository.findByEmployeeIdAndApplicationType(1L, "ONBOARDING"))
                .thenReturn(Optional.empty());
        when(applicationRepository.save(any(ApplicationWorkFlow.class))).thenReturn(savedApplication);

        ApplicationResponse response = applicationService.createApplication(request);

        assertThat(response.getId()).isEqualTo(10);
        assertThat(response.getEmployeeId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo("Open");
        assertThat(response.getCurrentStep()).isEqualTo("ONBOARDING_SUBMITTED");

        verify(applicationRepository).save(any(ApplicationWorkFlow.class));
        verify(documentRepository).saveAll(anyList());
    }

    @Test
    void createApplication_seedsOnboardingDocumentsFromTemplateCatalog() {
        ApplicationRequest request = ApplicationRequest.builder()
                .employeeId(1L)
                .applicationType("ONBOARDING")
                .build();
        ApplicationWorkFlow savedApplication = application(10, 1L, "Open", "ONBOARDING");

        when(applicationRepository.findByEmployeeIdAndApplicationType(1L, "ONBOARDING"))
                .thenReturn(Optional.empty());
        when(applicationRepository.save(any(ApplicationWorkFlow.class))).thenReturn(savedApplication);

        applicationService.createApplication(request);

        ArgumentCaptor<List<DigitalDocument>> captor = ArgumentCaptor.forClass(List.class);
        verify(documentRepository).saveAll(captor.capture());
        List<DigitalDocument> documents = captor.getValue();
        assertThat(documents).extracting(DigitalDocument::getType)
                .containsExactly("W4", "I9", "COMPANY_POLICY", "DIRECT_DEPOSIT");
        assertThat(documents).extracting(DigitalDocument::getPath).containsOnly("");
        assertThat(documents).extracting(DigitalDocument::getApplicationWorkFlow).containsOnly(savedApplication);
    }

    @Test
    void createApplication_setsOptStemInitialStep() {
        ApplicationRequest request = ApplicationRequest.builder()
                .employeeId(1L)
                .applicationType("OPT_STEM")
                .build();
        ApplicationWorkFlow savedApplication = application(10, 1L, "Open", "OPT_STEM");
        savedApplication.setCurrentStep("I983_DOWNLOADED");

        when(applicationRepository.findByEmployeeIdAndApplicationType(1L, "OPT_STEM"))
                .thenReturn(Optional.empty());
        when(applicationRepository.save(any(ApplicationWorkFlow.class))).thenReturn(savedApplication);

        ApplicationResponse response = applicationService.createApplication(request);

        assertThat(response.getCurrentStep()).isEqualTo("I983_DOWNLOADED");
    }

    @Test
    void createApplication_throwsWhenEmployeeHasOngoingApplicationOfSameType() {
        ApplicationRequest request = ApplicationRequest.builder()
                .employeeId(1L)
                .applicationType("ONBOARDING")
                .build();

        when(applicationRepository.findByEmployeeIdAndApplicationType(1L, "ONBOARDING"))
                .thenReturn(Optional.of(application(10, 1L, "Open", "ONBOARDING")));

        assertThatThrownBy(() -> applicationService.createApplication(request))
                .isInstanceOf(DuplicateApplicationException.class)
                .hasMessageContaining("ongoing ONBOARDING application");
    }

    @Test
    void createApplication_allowsNewApplicationWhenPreviousSameTypeIsCompleted() {
        ApplicationRequest request = ApplicationRequest.builder()
                .employeeId(1L)
                .applicationType("ONBOARDING")
                .build();
        ApplicationWorkFlow savedApplication = application(11, 1L, "Open", "ONBOARDING");

        when(applicationRepository.findByEmployeeIdAndApplicationType(1L, "ONBOARDING"))
                .thenReturn(Optional.of(application(10, 1L, "Completed", "ONBOARDING")));
        when(applicationRepository.save(any(ApplicationWorkFlow.class))).thenReturn(savedApplication);

        ApplicationResponse response = applicationService.createApplication(request);

        assertThat(response.getId()).isEqualTo(11);
        assertThat(response.getStatus()).isEqualTo("Open");
    }

    @Test
    void getApplicationById_returnsApplication() {
        when(applicationRepository.findById(10)).thenReturn(Optional.of(application(10, 1L, "Open", "ONBOARDING")));

        ApplicationResponse response = applicationService.getApplicationById(10);

        assertThat(response.getId()).isEqualTo(10);
        assertThat(response.getApplicationType()).isEqualTo("ONBOARDING");
    }

    @Test
    void getApplicationById_throwsWhenMissing() {
        when(applicationRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.getApplicationById(99))
                .isInstanceOf(ApplicationNotFoundException.class)
                .hasMessageContaining("Application not found");
    }

    @Test
    void getApplicationsByEmployeeId_returnsEmployeeApplications() {
        when(applicationRepository.findByEmployeeId(1L))
                .thenReturn(List.of(application(10, 1L, "Open", "ONBOARDING")));

        List<ApplicationResponse> response = applicationService.getApplicationsByEmployeeId(1L);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getEmployeeId()).isEqualTo(1L);
    }

    @Test
    void getApplicationsByStatus_returnsMatchingApplications() {
        when(applicationRepository.findByStatus("Open"))
                .thenReturn(List.of(application(10, 1L, "Open", "ONBOARDING")));

        List<ApplicationResponse> response = applicationService.getApplicationsByStatus("Open");

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getStatus()).isEqualTo("Open");
    }

    @Test
    void getApplicationsByType_returnsMatchingApplications() {
        when(applicationRepository.findByApplicationType("OPT_STEM"))
                .thenReturn(List.of(application(20, 1L, "Open", "OPT_STEM")));

        List<ApplicationResponse> response = applicationService.getApplicationsByType("OPT_STEM");

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getApplicationType()).isEqualTo("OPT_STEM");
    }

    @Test
    void reviewApplication_updatesOpenApplication() {
        ApplicationWorkFlow application = application(10, 1L, "Open", "ONBOARDING");
        ApplicationReviewRequest request = ApplicationReviewRequest.builder()
                .status("Completed")
                .comment("Approved")
                .employeeEmail("employee@example.com")
                .build();

        when(applicationRepository.findById(10)).thenReturn(Optional.of(application));
        when(applicationRepository.save(application)).thenReturn(application);

        ApplicationResponse response = applicationService.reviewApplication(10, request);

        assertThat(response.getStatus()).isEqualTo("Completed");
        assertThat(response.getCurrentStep()).isEqualTo("Completed");
        assertThat(response.getComment()).isEqualTo("Approved");
        verify(applicationRepository).save(application);
        verify(emailNotificationService).sendApplicationReviewEmail(application, "employee@example.com");
    }

    @Test
    void reviewApplication_throwsWhenApplicationIsNotOpen() {
        ApplicationReviewRequest request = ApplicationReviewRequest.builder()
                .status("Completed")
                .build();

        when(applicationRepository.findById(10))
                .thenReturn(Optional.of(application(10, 1L, "Rejected", "ONBOARDING")));

        assertThatThrownBy(() -> applicationService.reviewApplication(10, request))
                .isInstanceOf(InvalidApplicationStatusException.class)
                .hasMessageContaining("only Open applications");
    }

    @Test
    void reviewApplication_throwsWhenReviewStatusIsInvalid() {
        ApplicationReviewRequest request = ApplicationReviewRequest.builder()
                .status("Open")
                .build();

        when(applicationRepository.findById(10))
                .thenReturn(Optional.of(application(10, 1L, "Open", "ONBOARDING")));

        assertThatThrownBy(() -> applicationService.reviewApplication(10, request))
                .isInstanceOf(InvalidApplicationStatusException.class)
                .hasMessageContaining("Status must be Completed or Rejected");
    }

    @Test
    void advanceWorkflow_updatesCurrentStepForOpenApplication() {
        ApplicationWorkFlow application = application(10, 1L, "Open", "OPT_STEM");
        AdvanceWorkflowRequest request = AdvanceWorkflowRequest.builder()
                .currentStep("I20_UPLOADED")
                .comment("Uploaded new I-20")
                .build();

        when(applicationRepository.findById(10)).thenReturn(Optional.of(application));
        when(applicationRepository.save(application)).thenReturn(application);

        ApplicationResponse response = applicationService.advanceWorkflow(10, request);

        assertThat(response.getCurrentStep()).isEqualTo("I20_UPLOADED");
        assertThat(response.getComment()).isEqualTo("Uploaded new I-20");
        verify(applicationRepository).save(application);
    }

    @Test
    void advanceWorkflow_throwsWhenApplicationIsNotOpen() {
        AdvanceWorkflowRequest request = AdvanceWorkflowRequest.builder()
                .currentStep("I20_UPLOADED")
                .build();

        when(applicationRepository.findById(10))
                .thenReturn(Optional.of(application(10, 1L, "Completed", "OPT_STEM")));

        assertThatThrownBy(() -> applicationService.advanceWorkflow(10, request))
                .isInstanceOf(InvalidApplicationStatusException.class)
                .hasMessageContaining("only Open applications can be advanced");
    }

    @Test
    void advanceWorkflow_throwsWhenMissing() {
        AdvanceWorkflowRequest request = AdvanceWorkflowRequest.builder()
                .currentStep("I20_UPLOADED")
                .build();

        when(applicationRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.advanceWorkflow(99, request))
                .isInstanceOf(ApplicationNotFoundException.class)
                .hasMessageContaining("Application not found");
    }

    private ApplicationWorkFlow application(Integer id, Long employeeId, String status, String type) {
        return ApplicationWorkFlow.builder()
                .id(id)
                .employeeId(employeeId)
                .status(status)
                .currentStep("ONBOARDING_SUBMITTED")
                .applicationType(type)
                .comment(null)
                .createDate(LocalDateTime.now())
                .lastModificationDate(LocalDateTime.now())
                .build();
    }
}
