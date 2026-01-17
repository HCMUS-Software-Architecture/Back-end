package org.example.userservice.exception;

public class RefreshTokenNotExist extends RuntimeException {
    public RefreshTokenNotExist(String message) {
        super(message);
    }
}
