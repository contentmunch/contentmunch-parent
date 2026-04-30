package com.contentmunch.authentication.service;

import java.util.Optional;

import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.contentmunch.authentication.config.AuthConfigProperties;
import com.contentmunch.authentication.model.ContentmunchUser;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PropertyUserDetailsService extends ContentmunchUserDetailsService {
    private final AuthConfigProperties authConfig;

    @Override
    protected ContentmunchUser loadContentmunchUser(String username){
        return Optional.ofNullable(authConfig.users().get(username))
                .orElseThrow(() -> new UsernameNotFoundException(username));
    }
}
