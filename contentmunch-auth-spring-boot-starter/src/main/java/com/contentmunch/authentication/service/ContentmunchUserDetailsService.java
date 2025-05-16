package com.contentmunch.authentication.service;

import java.util.Optional;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.contentmunch.authentication.config.AuthConfigProperties;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ContentmunchUserDetailsService implements UserDetailsService {
    private final AuthConfigProperties authConfig;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException{
        return Optional.ofNullable(authConfig.users().get(username))
                .orElseThrow(() -> new UsernameNotFoundException(username));
    }
}
