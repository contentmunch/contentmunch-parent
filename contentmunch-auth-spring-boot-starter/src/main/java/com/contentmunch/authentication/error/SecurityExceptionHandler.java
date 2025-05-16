package com.contentmunch.authentication.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.contentmunch.foundation.error.ErrorResponse;

import lombok.extern.slf4j.Slf4j;

@ControllerAdvice
@Slf4j
public class SecurityExceptionHandler {

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex){
        log.warn("Bad credentials: {}",ex.getMessage());
        return unauthorized("Invalid username or password","BAD_CREDENTIALS");
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUsernameNotFound(UsernameNotFoundException ex){
        log.warn("Username not found: {}",ex.getMessage());
        return unauthorized("Username not found","USERNAME_NOT_FOUND");
    }

    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleMissingCredentials(AuthenticationCredentialsNotFoundException ex){
        log.warn("Credentials missing: {}",ex.getMessage());
        return unauthorized("Authentication credentials not found","CREDENTIALS_MISSING");
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabledUser(DisabledException ex){
        log.warn("Account disabled: {}",ex.getMessage());
        return forbidden("Your account is disabled","ACCOUNT_DISABLED");
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ErrorResponse> handleLockedUser(LockedException ex){
        log.warn("Account locked: {}",ex.getMessage());
        return forbidden("Your account is locked","ACCOUNT_LOCKED");
    }

    @ExceptionHandler(CredentialsExpiredException.class)
    public ResponseEntity<ErrorResponse> handleExpiredCredentials(CredentialsExpiredException ex){
        log.warn("Credentials expired: {}",ex.getMessage());
        return forbidden("Your credentials have expired","CREDENTIALS_EXPIRED");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex){
        log.warn("Access denied: {}",ex.getMessage());
        return forbidden("You do not have permission","ACCESS_DENIED");
    }

    @ExceptionHandler(AuthenticationServiceException.class)
    public ResponseEntity<ErrorResponse> handleAuthServiceError(AuthenticationServiceException ex){
        log.error("Internal auth error",ex);
        return serverError();
    }

    @ExceptionHandler(InsufficientAuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientAuth(InsufficientAuthenticationException ex){
        log.warn("Insufficient authentication: {}",ex.getMessage());
        return forbidden("Insufficient authentication","INSUFFICIENT_AUTH");
    }

    private ResponseEntity<ErrorResponse> unauthorized(String msg,String code){
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.builder().errorMessage(msg).errorCode(code).build());
    }

    private ResponseEntity<ErrorResponse> forbidden(String msg,String code){
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.builder().errorMessage(msg).errorCode(code).build());
    }

    private ResponseEntity<ErrorResponse> serverError(){
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorResponse.builder()
                .errorMessage("Authentication service error").errorCode("AUTH_SERVICE_ERROR").build());
    }
}
