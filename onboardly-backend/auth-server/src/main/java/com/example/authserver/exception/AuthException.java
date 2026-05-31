package com.example.authserver.exception;

public class AuthException extends RuntimeException {
    public AuthException(String message) {
        super(message);
    }
}
