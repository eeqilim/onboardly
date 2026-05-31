package com.example.applicationservice.service;

import com.example.applicationservice.dto.event.VisaWorkflowEvent;
import com.example.applicationservice.dto.request.AdvanceWorkflowRequest;
import com.example.applicationservice.exception.InvalidApplicationStatusException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class VisaWorkflowEventConsumer {

    private final ObjectMapper objectMapper;
    private final ApplicationService applicationService;

    @KafkaListener(topics = "${app.visa.workflow.topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(ConsumerRecord<String, String> record) throws JsonProcessingException {
        VisaWorkflowEvent event = objectMapper.readValue(record.value(), VisaWorkflowEvent.class);
        log.info("Received VisaWorkflowEvent eventType={} userId={}", event.eventType(), event.userId());

        switch (event.eventType()) {
            case "I20_UPLOADED", "OPT_RECEIPT_UPLOADED", "OPT_EAD_UPLOADED" ->
                    applicationService.advanceOptStemWorkflowByUserId(
                            event.userId(),
                            AdvanceWorkflowRequest.builder()
                                    .currentStep(event.eventType())
                                    .comment(commentFor(event))
                                    .build());
            case "STEM_OPT_APPROVED" ->
                    applicationService.reviewOptStemWorkflowByUserId(
                            event.userId(), "Completed", commentFor(event));
            case "STEM_OPT_REJECTED" ->
                    applicationService.reviewOptStemWorkflowByUserId(
                            event.userId(), "Rejected", commentFor(event));
            default -> throw new InvalidApplicationStatusException(
                    "Unsupported visa workflow event type: " + event.eventType());
        }
    }

    private String commentFor(VisaWorkflowEvent event) {
        if (event.metadata() != null && event.metadata().containsKey("hrFeedback")) {
            return Objects.toString(event.metadata().get("hrFeedback"), "");
        }
        return "EmployeeService event: " + event.eventType();
    }
}
