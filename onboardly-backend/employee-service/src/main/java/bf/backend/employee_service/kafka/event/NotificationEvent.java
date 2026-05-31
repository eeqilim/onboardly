package bf.backend.employee_service.kafka.event;

import java.util.Map;

// notificationType values: ONBOARDING_APPROVED, ONBOARDING_REJECTED,
//                          STEM_OPT_STEP_NEXT, STEM_OPT_APPROVED, STEM_OPT_REJECTED,
//                          REGISTRATION_TOKEN
public record NotificationEvent(
        Long employeeId,
        String employeeEmail,
        String notificationType,
        String subject,
        Map<String, Object> templateData,
        Long timestampMillis
) {}
