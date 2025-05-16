package com.contentmunch.foundation.threads;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnMissingBean(name = "virtualThreadExecutor")
public class VirtualThreadAutoConfiguration {
    @Bean
    public Executor virtualThreadExecutor(){
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
