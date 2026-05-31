package bf.backend.email_service.event;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailEvent {
    private String to;

    private String subject;

    private String body;
}