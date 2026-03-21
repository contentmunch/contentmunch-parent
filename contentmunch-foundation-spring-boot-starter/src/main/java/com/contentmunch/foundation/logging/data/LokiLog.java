package com.contentmunch.foundation.logging.data;

import lombok.Builder;

@Builder
public record LokiLog(String timestamp, String log) {
}
