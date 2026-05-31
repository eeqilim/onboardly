package bf.backend.employee_service.kafka.event;

public record EmailEvent(String to, String subject, String body) {}
