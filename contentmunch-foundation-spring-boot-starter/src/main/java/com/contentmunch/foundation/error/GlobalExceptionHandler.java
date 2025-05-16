package com.contentmunch.foundation.error;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import lombok.extern.slf4j.Slf4j;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(ServerException.class)
    public ResponseEntity<ErrorResponse> handleServerException(ServerException ex){
        log.error("Server exception: {}",ex.getErrorMessage(),ex);
        return ResponseEntity
                .status(Optional.ofNullable(ex.getErrorMessage().status()).orElse(HttpStatus.INTERNAL_SERVER_ERROR))
                .body(ErrorResponse.builder().errorMessage(ex.getErrorMessage().message())
                        .errorCode(ex.getErrorMessage().code()).build());
    }

    @ExceptionHandler(ClientException.class)
    public ResponseEntity<ErrorResponse> handleClientException(ClientException ex){
        log.error("Client exception: {} with error: {}",ex.getErrorMessage(),ex.getMessage());
        return ResponseEntity.status(Optional.ofNullable(ex.getErrorMessage().status()).orElse(HttpStatus.BAD_REQUEST))
                .body(ErrorResponse.builder().errorMessage(ex.getErrorMessage().message())
                        .errorCode(ex.getErrorMessage().code()).build());

    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex){
        log.error("Business exception: {} with error: {}",ex.getErrorMessage(),ex.getMessage());
        return ResponseEntity
                .status(Optional.ofNullable(ex.getErrorMessage().status()).orElse(HttpStatus.UNPROCESSABLE_ENTITY))
                .body(ErrorResponse.builder().errorMessage(ex.getErrorMessage().message())
                        .errorCode(ex.getErrorMessage().code()).build());

    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnhandledException(Exception ex){
        log.error("Unhandled exception",ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.builder().errorMessage(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                        .errorCode(HttpStatus.INTERNAL_SERVER_ERROR.name()).build());

    }

}
