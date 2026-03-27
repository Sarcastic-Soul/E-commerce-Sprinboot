package com.anish.e_commerce.exception;

import com.anish.e_commerce.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // Handle @Valid Validation Errors
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationExceptions(
        MethodArgumentNotValidException ex,
        HttpServletRequest request
    ) {
        Map<String, String> errors = new HashMap<>();
        ex
            .getBindingResult()
            .getAllErrors()
            .forEach(error -> {
                String fieldName = ((FieldError) error).getField();
                String errorMessage = error.getDefaultMessage();
                errors.put(fieldName, errorMessage);
            });

        ApiErrorResponse response = buildErrorResponse(
            HttpStatus.BAD_REQUEST,
            "Validation Failed",
            errors.toString(),
            request.getRequestURI()
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // Handle Authentication/Security Errors
    @ExceptionHandler(
        { AuthenticationException.class, AccessDeniedException.class }
    )
    public ResponseEntity<ApiErrorResponse> handleSecurityExceptions(
        Exception ex,
        HttpServletRequest request
    ) {
        HttpStatus status =
            ex instanceof AuthenticationException
                ? HttpStatus.UNAUTHORIZED
                : HttpStatus.FORBIDDEN;
        ApiErrorResponse response = buildErrorResponse(
            status,
            status.getReasonPhrase(),
            ex.getMessage(),
            request.getRequestURI()
        );
        return new ResponseEntity<>(response, status);
    }

    // Handle general Runtime Exceptions (like "Product not found")
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiErrorResponse> handleRuntimeExceptions(
        RuntimeException ex,
        HttpServletRequest request
    ) {
        log.error("Runtime exception occurred: ", ex);
        ApiErrorResponse response = buildErrorResponse(
            HttpStatus.BAD_REQUEST,
            "Bad Request",
            ex.getMessage(),
            request.getRequestURI()
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
        ConstraintViolationException ex,
        HttpServletRequest request
    ) {
        ApiErrorResponse response = buildErrorResponse(
            HttpStatus.BAD_REQUEST,
            "Validation Failed",
            ex.getMessage(),
            request.getRequestURI()
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // Fallback for any other unhandled exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGlobalExceptions(
        Exception ex,
        HttpServletRequest request
    ) {
        log.error("Unhandled exception occurred: ", ex);
        ApiErrorResponse response = buildErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Internal Server Error",
            "An unexpected error occurred.",
            request.getRequestURI()
        );
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ApiErrorResponse buildErrorResponse(
        HttpStatus status,
        String error,
        String message,
        String path
    ) {
        return ApiErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(status.value())
            .error(error)
            .message(message)
            .path(path)
            .build();
    }
}
