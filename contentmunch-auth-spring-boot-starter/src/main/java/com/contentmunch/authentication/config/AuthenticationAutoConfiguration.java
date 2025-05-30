package com.contentmunch.authentication.config;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.contentmunch.authentication.service.ContentmunchUserDetailsService;
import com.contentmunch.authentication.service.CookieService;
import com.contentmunch.authentication.service.TokenizationService;

@Configuration
@EnableConfigurationProperties({AuthConfigProperties.class})
@ImportAutoConfiguration(classes = {CookieService.class, ContentmunchUserDetailsService.class,
        TokenizationService.class})
public class AuthenticationAutoConfiguration {
}
