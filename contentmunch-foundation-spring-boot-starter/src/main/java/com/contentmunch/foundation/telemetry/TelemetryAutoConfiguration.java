package com.contentmunch.foundation.telemetry;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@Configuration
@ConditionalOnWebApplication
@AutoConfigureAfter(name = "org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration")
@EnableConfigurationProperties(TelemetryProperties.class)
@ConditionalOnProperty(name = "contentmunch.telemetry.enabled", havingValue = "true", matchIfMissing = true)
public class TelemetryAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @Order(Ordered.HIGHEST_PRECEDENCE + 10)
    public TelemetryHeaderFilter telemetryHeaderFilter(){
        return new TelemetryHeaderFilter();
    }
}
