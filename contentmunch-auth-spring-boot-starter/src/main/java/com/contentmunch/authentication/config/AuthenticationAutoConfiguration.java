package com.contentmunch.authentication.config;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.contentmunch.authentication.controller.AuthController;
import com.contentmunch.authentication.error.SecurityExceptionHandler;
import com.contentmunch.authentication.service.ContentmunchUserDetailsService;
import com.contentmunch.authentication.service.CookieService;
import com.contentmunch.authentication.service.PropertyUserDetailsService;
import com.contentmunch.authentication.service.TokenizationService;

@Configuration
@EnableConfigurationProperties({AuthConfigProperties.class})
@ImportAutoConfiguration(classes = {CookieService.class, TokenizationService.class, SecurityExceptionHandler.class,
        AuthController.class})
public class AuthenticationAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(ContentmunchUserDetailsService.class)
    public ContentmunchUserDetailsService propertyUserDetailsService(AuthConfigProperties authConfig){
        return new PropertyUserDetailsService(authConfig);
    }
}
