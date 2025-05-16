package com.contentmunch.authentication.service;

import java.time.Duration;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import com.contentmunch.authentication.config.AuthConfigProperties;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CookieService {
    private final AuthConfigProperties authConfig;

    public ResponseCookie cookieFromToken(String token,int maxAge){
        return ResponseCookie.from(authConfig.cookie().name(),token).httpOnly(authConfig.cookie().httpOnly())
                .secure(authConfig.cookie().secure()).path(authConfig.cookie().path())
                .maxAge(Duration.ofMinutes(maxAge)).sameSite(authConfig.cookie().sameSite().getValue()).build();
    }

    public ResponseCookie cookieFromToken(String token){
        return cookieFromToken(token,authConfig.maxAgeInMinutes());
    }

}
