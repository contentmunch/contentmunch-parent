package com.contentmunch.foundation.error;

import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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

    /**
     * Spring based exceptions
     **/
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(NoResourceFoundException ex){
        log.warn("Resource not found: {}",ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.builder().errorMessage(ex.getMessage()).errorCode("RESOURCE_NOT_FOUND").build());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex){
        log.error("HTTP method not supported: {}",ex.getMethod());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ErrorResponse.builder()
                        .errorMessage("Method " + ex.getMethod() + " not allowed for this endpoint")
                        .errorCode("METHOD_NOT_ALLOWED").build());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParams(MissingServletRequestParameterException ex){
        log.error("Missing request parameter: {}",ex.getParameterName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.builder().errorMessage("Missing request parameter: " + ex.getParameterName())
                        .errorCode("MISSING_PARAMETER").build());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleJsonParseError(HttpMessageNotReadableException ex){
        log.error("Malformed JSON request",ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ErrorResponse.builder().errorMessage("Malformed JSON request").errorCode("MALFORMED_JSON").build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex){
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage()).collect(Collectors.joining(", "));

        log.error("Validation failed: {}",errorMessage);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.builder().errorMessage(errorMessage).errorCode("VALIDATION_ERROR").build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnhandledException(Exception ex){
        log.error("Unhandled exception",ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.builder().errorMessage(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                        .errorCode(HttpStatus.INTERNAL_SERVER_ERROR.name()).build());

    }

}
