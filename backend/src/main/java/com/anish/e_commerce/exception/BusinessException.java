package com.anish.e_commerce.exception;

/**
 * Thrown for expected business-rule violations (empty cart, insufficient stock,
 * invalid state transition). Mapped to HTTP 400 by {@link GlobalExceptionHandler}.
 * A plain, un-typed RuntimeException is treated as an unexpected bug instead and
 * falls through to the generic 500 handler.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
