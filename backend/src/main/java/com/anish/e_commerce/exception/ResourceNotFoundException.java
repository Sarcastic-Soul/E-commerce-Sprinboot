package com.anish.e_commerce.exception;

/**
 * Thrown when a requested entity does not exist. Mapped to HTTP 404 by
 * {@link GlobalExceptionHandler}. See {@link BusinessException} for 400s and
 * plain RuntimeException for unexpected bugs (mapped to 500).
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
