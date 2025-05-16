package com.contentmunch.authentication.config;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Configuration;

import com.contentmunch.authentication.controller.AuthController;
import com.contentmunch.authentication.error.SecurityExceptionHandler;
import com.contentmunch.authentication.service.ContentmunchUserDetailsService;
import com.contentmunch.authentication.service.CookieService;
import com.contentmunch.authentication.service.TokenizationService;

@Configuration
@ImportAutoConfiguration(classes = {SecurityExceptionHandler.class, SecurityConfig.class, AuthController.class,
        CookieService.class, ContentmunchUserDetailsService.class, TokenizationService.class})
public class AuthenticationAutoConfiguration {
}
