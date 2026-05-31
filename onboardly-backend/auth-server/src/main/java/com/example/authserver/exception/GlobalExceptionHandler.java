package com.example.authserver.exception;
import com.example.authserver.dto.response.DataResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    // can not find user
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<DataResponse> handleUserNotFound(UserNotFoundException ex) {
        DataResponse body = DataResponse.builder().message(ex.getMessage()).build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    // login failed
    @ExceptionHandler(AuthException.class)
    public ResponseEntity<DataResponse> handleAuthException(AuthException ex) {
        DataResponse body = DataResponse.builder().message(ex.getMessage()).build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    // user or email has existed
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<DataResponse> handleUserAlreadyExists(UserAlreadyExistsException ex) {
        DataResponse body = DataResponse.builder().message(ex.getMessage()).build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    //Invalid Token
    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<DataResponse> handleInvalidToken(InvalidTokenException ex) {
        DataResponse body = DataResponse.builder().message(ex.getMessage()).build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // Token Expired
    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<DataResponse> handleTokenExpired(TokenExpiredException ex) {
        DataResponse body = DataResponse.builder().message(ex.getMessage()).build();
        return ResponseEntity.status(HttpStatus.GONE).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<DataResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getDefaultMessage())
                .findFirst()
                .orElse("Validation failed");
        DataResponse body = DataResponse.builder().message(message).build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // other
    @ExceptionHandler(Exception.class)
    public ResponseEntity<DataResponse> handleException(Exception ex) {
        DataResponse body = DataResponse.builder().message(ex.getMessage()).build();
        return ResponseEntity.status(500).body(body);
    }
}
