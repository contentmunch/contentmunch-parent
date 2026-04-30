package com.contentmunch.authentication.config;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetailsService;

import com.contentmunch.authentication.service.ContentmunchUserDetailsService;
import com.contentmunch.authentication.service.CookieService;
import com.contentmunch.authentication.service.TokenizationService;

@Configuration
@EnableConfigurationProperties({AuthConfigProperties.class})
@ImportAutoConfiguration(classes = {CookieService.class, ContentmunchUserDetailsService.class,
        TokenizationService.class})
public class AuthenticationAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(UserDetailsService.class)
    public UserDetailsService userDetailsService(AuthConfigProperties authConfig){
        return new ContentmunchUserDetailsService(authConfig);
    }
}
