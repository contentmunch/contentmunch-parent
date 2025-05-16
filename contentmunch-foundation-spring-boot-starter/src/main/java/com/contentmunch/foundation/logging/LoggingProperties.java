package com.contentmunch.foundation.logging;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "contentmunch.logging")
public record LoggingProperties(boolean enabled) {
}
