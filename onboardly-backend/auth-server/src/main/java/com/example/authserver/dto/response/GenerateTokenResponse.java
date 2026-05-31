package com.example.authserver.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class GenerateTokenResponse {
    private String token;
    private String email;
    private LocalDateTime expirationDate;
}
