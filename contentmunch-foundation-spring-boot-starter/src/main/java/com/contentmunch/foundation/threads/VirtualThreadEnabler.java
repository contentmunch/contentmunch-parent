package com.contentmunch.foundation.threads;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;

public class VirtualThreadEnabler implements EnvironmentPostProcessor {
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment,SpringApplication application){
        environment.getSystemProperties().putIfAbsent("spring.threads.virtual.enabled","true");
    }
}
