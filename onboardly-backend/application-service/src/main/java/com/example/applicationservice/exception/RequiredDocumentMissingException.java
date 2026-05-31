package com.example.applicationservice.exception;

public class RequiredDocumentMissingException extends RuntimeException {
    public RequiredDocumentMissingException(String message) {
        super(message);
    }
}