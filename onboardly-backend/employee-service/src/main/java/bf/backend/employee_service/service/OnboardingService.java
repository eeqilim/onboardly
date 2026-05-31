package bf.backend.employee_service.service;

import bf.backend.employee_service.dto.request.AddCommentRequest;
import bf.backend.employee_service.dto.request.OnboardingApplicationRequest;
import bf.backend.employee_service.dto.request.ReviewApplicationRequest;
import bf.backend.employee_service.dto.response.ApplicationCommentResponse;
import bf.backend.employee_service.dto.response.HrOnboardingProfileResponse;
import bf.backend.employee_service.dto.response.OnboardingApplicationResponse;

import java.util.List;

public interface OnboardingService {

    OnboardingApplicationResponse getMyApplication();

    OnboardingApplicationResponse startApplication();

    OnboardingApplicationResponse submitApplication(OnboardingApplicationRequest req);

    OnboardingApplicationResponse reviewApplication(String applicationId, ReviewApplicationRequest req);

    List<OnboardingApplicationResponse> listPendingApplications();

    ApplicationCommentResponse addComment(String applicationId, AddCommentRequest req);

    HrOnboardingProfileResponse getHrOnboardingProfile(String employeeId);
}
