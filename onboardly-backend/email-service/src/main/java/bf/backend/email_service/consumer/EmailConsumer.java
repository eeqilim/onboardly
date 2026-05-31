package bf.backend.email_service.consumer;

import bf.backend.email_service.event.EmailEvent;
import bf.backend.email_service.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailConsumer {
    private final EmailService emailService;
    private final String groupId = "email-group";
    private final String topic = "email-topic";

    @KafkaListener(topics = topic, groupId = groupId)
    public void consumeEmailEvent(EmailEvent event){
        log.info("Received email event: {}", event);
        emailService.sendEmail(event.getTo(), event.getSubject(), event.getBody());
    }
}
