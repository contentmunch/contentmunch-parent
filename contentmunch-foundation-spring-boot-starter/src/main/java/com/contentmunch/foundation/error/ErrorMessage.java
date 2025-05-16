package com.contentmunch.foundation.error;

import org.springframework.http.HttpStatus;

import lombok.Builder;

@Builder
public record ErrorMessage(String message, String code, HttpStatus status) {
}
