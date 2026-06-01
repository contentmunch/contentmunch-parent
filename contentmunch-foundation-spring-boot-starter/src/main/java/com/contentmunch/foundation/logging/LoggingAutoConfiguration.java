package com.contentmunch.foundation.logging;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
@ConditionalOnWebApplication
@AutoConfigureAfter(name = "org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration")
@EnableConfigurationProperties(LoggingProperties.class)
@ConditionalOnProperty(name = "contentmunch.logging.enabled", havingValue = "true", matchIfMissing = true)
public class LoggingAutoConfiguration {

    @Bean
    public FilterRegistrationBean<RequestResponseLoggingFilter> loggingFilter() {
        FilterRegistrationBean<RequestResponseLoggingFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new RequestResponseLoggingFilter());

        registrationBean.setOrder(Ordered.LOWEST_PRECEDENCE);

        return registrationBean;
    }
}
