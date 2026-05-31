package com.example.applicationservice.exception;

import com.example.applicationservice.dto.response.DataResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApplicationNotFoundException.class)
    public ResponseEntity<DataResponse> handleApplicationNotFoundException(ApplicationNotFoundException ex) {
        DataResponse body = DataResponse.builder().message(ex.getMessage()).build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(DocumentNotFoundException.class)
    public ResponseEntity<DataResponse> handleDocumentNotFoundException(DocumentNotFoundException ex) {
        DataResponse body = DataResponse.builder().message(ex.getMessage()).build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(DuplicateApplicationException.class)
    public ResponseEntity<DataResponse> handleDuplicateApplicationException(DuplicateApplicationException ex) {
        DataResponse body = DataResponse.builder().message(ex.getMessage()).build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<DataResponse> handleUnauthorizedAccessException(UnauthorizedAccessException ex) {
        DataResponse body = DataResponse.builder().message(ex.getMessage()).build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(InvalidApplicationStatusException.class)
    public ResponseEntity<DataResponse> handleInvalidApplicationStatusException(InvalidApplicationStatusException ex) {
        DataResponse body = DataResponse.builder().message(ex.getMessage()).build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(RequiredDocumentMissingException.class)
    public ResponseEntity<DataResponse> handleRequiredDocumentMissingException(RequiredDocumentMissingException ex) {
        DataResponse body = DataResponse.builder().message(ex.getMessage()).build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<DataResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("Validation error");
        DataResponse body = DataResponse.builder().message(message).build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<DataResponse> handleException(Exception ex) {
        DataResponse body = DataResponse.builder().message(ex.getMessage()).build();
        return ResponseEntity.status(500).body(body);
    }
}
