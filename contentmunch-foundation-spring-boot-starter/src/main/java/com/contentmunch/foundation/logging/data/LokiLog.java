package com.contentmunch.foundation.logging.data;

import lombok.Builder;

@Builder
public record LokiLog(long timestamp, String log, String traceId, String spanId) {
}
