package com.example.authserver.service;

import com.example.authserver.dto.request.GenerateTokenRequest;
import com.example.authserver.dto.request.LoginRequest;
import com.example.authserver.dto.request.RegisterRequest;
import com.example.authserver.dto.response.AuthResponse;
import com.example.authserver.dto.response.GenerateTokenResponse;

public interface AuthService {
    GenerateTokenResponse generateRegistrationToken(GenerateTokenRequest request);
    AuthResponse login(LoginRequest request);
    void register(RegisterRequest request);
    boolean validateRegistrationToken(String token);
}
