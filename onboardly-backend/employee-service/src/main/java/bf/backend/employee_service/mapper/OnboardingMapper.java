package bf.backend.employee_service.mapper;

import bf.backend.employee_service.dto.response.ApplicationCommentResponse;
import bf.backend.employee_service.dto.response.OnboardingApplicationResponse;
import bf.backend.employee_service.entity.ApplicationStatus;
import bf.backend.employee_service.entity.Employee;
import bf.backend.employee_service.entity.OnboardingApplication;

import java.util.List;

public final class OnboardingMapper {

    private OnboardingMapper() {}

    public static OnboardingApplicationResponse toResponse(OnboardingApplication app) {
        List<ApplicationCommentResponse> comments = app.getComments() == null ? List.of() :
                app.getComments().stream()
                        .map(c -> new ApplicationCommentResponse(c.getId(), c.getAuthorId(), c.getCreatedAt(), c.getContent()))
                        .toList();
        return new OnboardingApplicationResponse(
                app.getId(),
                app.getEmployeeId(),
                app.getApplicationWorkflowId(),
                app.getStatus(),
                app.getHrFeedback(),
                app.getSubmittedAt(),
                app.getReviewedAt(),
                app.getReviewedBy(),
                app.getCreatedAt(),
                app.getUpdatedAt(),
                comments
        );
    }

    public static OnboardingApplication newApplication(Employee employee) {
        OnboardingApplication app = new OnboardingApplication();
        app.setEmployeeId(employee.getId());
        app.setStatus(ApplicationStatus.NOT_STARTED);
        return app;
    }
}
