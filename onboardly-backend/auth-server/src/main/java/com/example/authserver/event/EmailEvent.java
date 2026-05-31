package com.example.authserver.event;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class EmailEvent {
    private String to;
    private String subject;
    private String body;
}
