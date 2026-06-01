package com.contentmunch.foundation.error;

import lombok.Builder;
import org.springframework.http.HttpStatus;

@Builder
public record ErrorMessage(String message, String code, HttpStatus status) {}
