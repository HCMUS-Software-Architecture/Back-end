package org.example.userservice.exception;

public class RefreshTokenRevokeException extends RuntimeException {
    public RefreshTokenRevokeException(String message) {
        super(message);
    }
}
