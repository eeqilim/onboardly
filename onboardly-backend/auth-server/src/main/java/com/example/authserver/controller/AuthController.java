package com.example.authserver.controller;

import com.example.authserver.dto.request.GenerateTokenRequest;
import com.example.authserver.dto.request.LoginRequest;
import com.example.authserver.dto.request.RegisterRequest;
import com.example.authserver.dto.response.AuthResponse;
import com.example.authserver.dto.response.DataResponse;
import com.example.authserver.dto.response.GenerateTokenResponse;
import com.example.authserver.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
@Tag(name = "Authentication", description = "Login and Registration")
public class AuthController {
    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/token/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public GenerateTokenResponse generateToken(@Valid @RequestBody GenerateTokenRequest request) {
        return authService.generateRegistrationToken(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
    }

    @GetMapping("/token/validate")
    public DataResponse validateToken(@RequestParam String token) {
        authService.validateRegistrationToken(token);
        return DataResponse.builder().message("Token is valid").build();
    }
}
