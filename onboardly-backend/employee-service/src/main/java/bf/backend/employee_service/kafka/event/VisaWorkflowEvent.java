package bf.backend.employee_service.kafka.event;

import java.util.Map;

// eventType values: I20_UPLOADED, OPT_RECEIPT_UPLOADED, OPT_EAD_UPLOADED,
//                   STEM_OPT_APPROVED, STEM_OPT_REJECTED
public record VisaWorkflowEvent(
        Long userId,
        String eventType,
        String documentS3Key,
        Long timestampMillis,
        Map<String, Object> metadata
) {}
