package com.example.authserver.dto.response;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class AuthResponse {
    private String token;
    private String username;
    private Long userId;
    private String email;
    private String role;
}
