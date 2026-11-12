package com.cssms.exception;

/** A business rule was violated (duplicate, insufficient stock, credit limit...). Mapped to HTTP 409. */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
