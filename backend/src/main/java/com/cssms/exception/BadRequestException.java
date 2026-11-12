package com.cssms.exception;

/** The request is syntactically fine but semantically invalid. Mapped to HTTP 400. */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
