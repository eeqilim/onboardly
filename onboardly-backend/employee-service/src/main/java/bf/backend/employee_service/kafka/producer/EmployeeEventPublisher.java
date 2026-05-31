package bf.backend.employee_service.kafka.producer;

import bf.backend.employee_service.kafka.event.EmailEvent;
import bf.backend.employee_service.kafka.event.NotificationEvent;
import bf.backend.employee_service.kafka.event.VisaWorkflowEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmployeeEventPublisher {

    private static final String VISA_TOPIC = "visa-workflow-events";
    private static final String NOTIFICATION_TOPIC = "notification-events";
    private static final String EMAIL_TOPIC = "email-topic";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishVisaWorkflowEvent(VisaWorkflowEvent event) {
        String key = event.userId().toString();
        kafkaTemplate.send(VISA_TOPIC, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish VisaWorkflowEvent [{}] for employeeId={}: {}",
                                event.eventType(), event.userId(), ex.getMessage());
                    } else {
                        log.debug("Published VisaWorkflowEvent [{}] employeeId={} -> {}:{} offset={}",
                                event.eventType(), event.userId(),
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }

    public void publishEmailEvent(EmailEvent event) {
        kafkaTemplate.send(EMAIL_TOPIC, event.to(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish EmailEvent to={}: {}", event.to(), ex.getMessage());
                    } else {
                        log.debug("Published EmailEvent to={} -> {}:{} offset={}",
                                event.to(),
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }

    public void publishNotificationEvent(NotificationEvent event) {
        String key = event.employeeId().toString();
        kafkaTemplate.send(NOTIFICATION_TOPIC, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish NotificationEvent [{}] for employeeId={}: {}",
                                event.notificationType(), event.employeeId(), ex.getMessage());
                    } else {
                        log.debug("Published NotificationEvent [{}] employeeId={} -> {}:{} offset={}",
                                event.notificationType(), event.employeeId(),
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
