package com.contentmunch.foundation.error;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(GlobalExceptionHandler.class)
public class ExceptionHandlerAutoConfiguration {
}
