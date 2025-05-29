package com.contentmunch.authentication.service;

import java.time.Duration;
import java.util.Arrays;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import com.contentmunch.authentication.config.AuthConfigProperties;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CookieService {
    private final AuthConfigProperties authConfig;

    public ResponseCookie cookieFromAccessToken(String token,int maxAge){
        return ResponseCookie.from(authConfig.cookie().name(),token).httpOnly(authConfig.cookie().httpOnly())
                .secure(authConfig.cookie().secure()).path(authConfig.cookie().path())
                .maxAge(Duration.ofMinutes(maxAge)).sameSite(authConfig.cookie().sameSite().getValue()).build();
    }

    public ResponseCookie cookieFromAccessToken(String token){
        return cookieFromAccessToken(token,authConfig.accessTokenMaxAgeInMinutes());
    }

    public ResponseCookie cookieFromRefreshToken(String token){
        return ResponseCookie.from(String.format("%s-refresh_token",authConfig.cookie().name()),token)
                .httpOnly(authConfig.cookie().httpOnly()).secure(authConfig.cookie().secure()).path("/api/auth/refresh")
                .maxAge(Duration.ofDays(authConfig.refreshTokenMaxAgeDays()))
                .sameSite(authConfig.cookie().sameSite().getValue()).build();
    }

    public String extractRefreshToken(HttpServletRequest request){
        if (request.getCookies() == null)
            return null;

        return Arrays.stream(request.getCookies())
                .filter(c -> c.getName().equals(String.format("%s-refresh_token",authConfig.cookie().name())))
                .map(Cookie::getValue).findFirst().orElse(null);
    }

}
