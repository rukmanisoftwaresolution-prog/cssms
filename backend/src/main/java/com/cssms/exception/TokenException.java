package com.cssms.exception;

/** Invalid, expired or reused refresh / reset token. Mapped to HTTP 401. */
public class TokenException extends RuntimeException {
    public TokenException(String message) {
        super(message);
    }
}
