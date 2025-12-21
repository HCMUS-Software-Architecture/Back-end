package com.example.backend.exception;

public class RefreshTokenNotExist extends RuntimeException {
    public RefreshTokenNotExist(String message) {
        super(message);
    }
}
