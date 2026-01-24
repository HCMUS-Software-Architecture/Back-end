package org.example.userservice.exception;

/**
 * Exception thrown when a password reset token is invalid, expired, or already used.
 */
public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException(String message) {
        super(message);
    }
}
