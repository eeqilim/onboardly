package bf.backend.employee_service.kafka;

import bf.backend.employee_service.kafka.event.NotificationEvent;
import bf.backend.employee_service.kafka.event.VisaWorkflowEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;

/** @deprecated replaced by {@link bf.backend.employee_service.kafka.producer.EmployeeEventPublisher} */
@Deprecated
@RequiredArgsConstructor
public class KafkaEventProducer {

    private static final String NOTIFICATION_TOPIC = "notification-events";
    private static final String VISA_TOPIC = "visa-workflow-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendNotificationEvent(NotificationEvent event) {
        kafkaTemplate.send(NOTIFICATION_TOPIC, String.valueOf(event.employeeId()), event);
    }

    public void sendVisaWorkflowEvent(VisaWorkflowEvent event) {
        kafkaTemplate.send(VISA_TOPIC, String.valueOf(event.userId()), event);
    }
}
