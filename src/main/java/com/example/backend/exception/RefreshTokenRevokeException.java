package com.example.backend.exception;

public class RefreshTokenRevokeException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public RefreshTokenRevokeException(String message) {
        super(message);
    }
}
