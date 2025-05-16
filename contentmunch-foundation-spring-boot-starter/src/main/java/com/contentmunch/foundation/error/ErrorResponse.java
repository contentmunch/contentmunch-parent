package com.contentmunch.foundation.error;

import lombok.Builder;

@Builder
public record ErrorResponse(String errorMessage, String errorCode) {
}
