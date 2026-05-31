package com.example.applicationservice.service;

import com.example.applicationservice.domain.ApplicationWorkFlow;
import com.example.applicationservice.dto.event.EmailEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationService {

    private final KafkaTemplate<String, EmailEvent> kafkaTemplate;

    @Value("${app.email.topic:email-topic}")
    private String emailTopic;

    public void sendApplicationReviewEmail(ApplicationWorkFlow application, String recipientEmail) {
        if (!StringUtils.hasText(recipientEmail)) {
            return;
        }

        EmailEvent event = EmailEvent.builder()
                .to(recipientEmail)
                .subject("Application " + application.getStatus())
                .body(buildReviewMessage(application))
                .build();

        try {
            kafkaTemplate.send(emailTopic, String.valueOf(application.getId()), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.warn("Failed to publish application review email event for application {}",
                                    application.getId(), ex);
                        }
                    });
        } catch (RuntimeException ex) {
            log.warn("Failed to publish application review email event for application {}",
                    application.getId(), ex);
        }
    }

    private String buildReviewMessage(ApplicationWorkFlow application) {
        StringBuilder message = new StringBuilder()
                .append("Your ")
                .append(application.getApplicationType())
                .append(" application has been ")
                .append(application.getStatus())
                .append(".");

        if (StringUtils.hasText(application.getComment())) {
            message.append(" HR comment: ").append(application.getComment());
        }

        return message.toString();
    }
}
