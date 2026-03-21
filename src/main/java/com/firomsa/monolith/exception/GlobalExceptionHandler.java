package com.firomsa.monolith.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.HttpMediaTypeException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import com.firomsa.monolith.v1.dto.ErrorResponseDTO;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponseDTO> handleBadCredentialsException(
            BadCredentialsException ex) {
        log.warn(ex.getMessage());
        var response = new ErrorResponseDTO(403, ex.getMessage(), LocalDateTime.now(), null);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(InvalidOtpException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvalidOtpException(InvalidOtpException ex) {
        log.warn(ex.getMessage());
        var response = new ErrorResponseDTO(400, ex.getMessage(), LocalDateTime.now(), null);
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDTO> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex) {
        log.warn(ex.getMessage());
        var response = new ErrorResponseDTO(400, ex.getMessage(), LocalDateTime.now(), null);
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidationException(ValidationException ex) {
        log.warn(ex.getMessage());
        var response = new ErrorResponseDTO(400, ex.getMessage(), LocalDateTime.now(), null);
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleUsernameNotFoundException(
            UsernameNotFoundException ex) {
        log.warn(ex.getMessage());
        var response = new ErrorResponseDTO(401, ex.getMessage(), LocalDateTime.now(), null);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(HttpMediaTypeException.class)
    public ResponseEntity<ErrorResponseDTO> handleHttpMediaTypeException(
            HttpMediaTypeException ex) {
        log.warn(ex.getMessage());
        var response = new ErrorResponseDTO(400, ex.getMessage(), LocalDateTime.now(), null);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ErrorResponseDTO> handleAuthorizationDeniedException(
            AuthorizationDeniedException ex) {
        log.warn(ex.getMessage());
        var response = new ErrorResponseDTO(403, ex.getMessage(), LocalDateTime.now(), null);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponseDTO> handleDisabledException(DisabledException ex) {
        log.warn("User is disabled: {}", ex.getMessage());
        var response = new ErrorResponseDTO(401, "User is disabled, " + ex.getMessage(),
                LocalDateTime.now(), null);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
        log.warn("Validation error: {}", errors);
        var response = new ErrorResponseDTO(400, "Validation failed for fields",
                LocalDateTime.now(), errors);
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleResourceNotFoundException(
            ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        var response = new ErrorResponseDTO(404, "Resource not found, " + ex.getMessage(),
                LocalDateTime.now(), null);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponseDTO> handleUserAlreadyExistsException(
            UserAlreadyExistsException ex) {
        var response = new ErrorResponseDTO(400, "User already exists, " + ex.getMessage(),
                LocalDateTime.now(), null);
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponseDTO> handleAuthenticationException(
            AuthenticationException ex) {
        var response = new ErrorResponseDTO(400,
                "Authentication error occurred, " + ex.getMessage(), LocalDateTime.now(), null);
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(MailException.class)
    public ResponseEntity<ErrorResponseDTO> handleMailException(MailException ex) {
        log.warn("Error when sending mail: {}", ex.getMessage());
        var response =
                new ErrorResponseDTO(503, "Error when sending mail", LocalDateTime.now(), null);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGenericException(Exception ex) {
        log.warn("An unexpected error occurred: {}", ex.getMessage(), ex);
        var response = new ErrorResponseDTO(500, "An unexpected error occurred on the server.",
                LocalDateTime.now(), null);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
