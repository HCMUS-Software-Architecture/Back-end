package com.example.backend.exception;

import com.example.backend.dto.ErrorResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<?> handleBadRequestException(BadRequestException ex) {
        log.error(ex.getMessage(), ex);
        return buildResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleUnexpectedError(Exception e) {
        log.error("Unexpected error occur: " + e.getMessage());
        return buildResponse("System error", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ErrorResponseDto> buildResponse(String message, HttpStatus httpStatus) {
        ErrorResponseDto responseDto = ErrorResponseDto.builder()
                .message(message)
                .status(httpStatus)
                .error(httpStatus.getReasonPhrase())
                .build();
        // httpStatus đang trùng 2 chỗ
        return ResponseEntity.status(httpStatus).body(responseDto);
    }
}
