package com.contentmunch.foundation.telemetry;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "contentmunch.telemetry")
public record TelemetryProperties(boolean enabled) {
}
