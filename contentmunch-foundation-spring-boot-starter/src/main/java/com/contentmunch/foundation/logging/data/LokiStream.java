package com.contentmunch.foundation.logging.data;

import lombok.Builder;

@Builder
public record LokiStream(String app, String environment, String traceId, String spanId) {
}
