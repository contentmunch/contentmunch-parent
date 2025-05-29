package com.contentmunch.authentication.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

import com.contentmunch.authentication.config.AuthConfigProperties;

class CookieServiceTest {

    private CookieService cookieService;

    @BeforeEach
    void setUp(){
        AuthConfigProperties.CookieConfig cookieConfig = AuthConfigProperties.CookieConfig.builder().name("auth-token")
                .sameSite(AuthConfigProperties.CookieConfig.SameSite.LAX).secure(true).httpOnly(true).path("/").build();

        AuthConfigProperties config = AuthConfigProperties.builder().accessTokenMaxAgeInMinutes(60)
                .refreshTokenMaxAgeDays(7).secret("dummy-secret").cookie(cookieConfig).users(Map.of()).build();

        cookieService = new CookieService(config);
    }

    @Test
    void shouldCreateCookieWithCustomMaxAge(){
        String token = "abc123";
        int customMaxAge = 10;

        ResponseCookie cookie = cookieService.cookieFromAccessToken(token,customMaxAge);

        assertThat(cookie.getName()).isEqualTo("auth-token");
        assertThat(cookie.getValue()).isEqualTo("abc123");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofMinutes(customMaxAge));
        assertThat(cookie.getSameSite()).isEqualTo("Lax");
    }

    @Test
    void shouldCreateCookieUsingDefaultMaxAge(){
        String token = "xyz456";

        ResponseCookie cookie = cookieService.cookieFromAccessToken(token);

        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofMinutes(60));
        assertThat(cookie.getName()).isEqualTo("auth-token");
        assertThat(cookie.getValue()).isEqualTo("xyz456");
    }

    @Test
    void shouldCreateCookieWithDifferentSameSiteValues(){
        for (AuthConfigProperties.CookieConfig.SameSite sameSite : AuthConfigProperties.CookieConfig.SameSite
                .values()) {
            var cookieConfig = AuthConfigProperties.CookieConfig.builder().name("auth-token").sameSite(sameSite)
                    .secure(true).httpOnly(true).path("/").build();

            var config = AuthConfigProperties.builder().accessTokenMaxAgeInMinutes(60).refreshTokenMaxAgeDays(7)
                    .secret("secret").cookie(cookieConfig).users(Map.of()).build();

            var service = new CookieService(config);

            ResponseCookie cookie = service.cookieFromAccessToken("token-" + sameSite.name(),5);
            assertThat(cookie.getSameSite()).isEqualTo(sameSite.getValue());
        }
    }

    @Test
    void shouldHandleZeroOrNegativeMaxAgeGracefully(){
        ResponseCookie cookie = cookieService.cookieFromAccessToken("token",0);
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofMinutes(0));
    }

    @Test
    void shouldHandleEmptyToken(){
        ResponseCookie cookie = cookieService.cookieFromAccessToken("",5);
        assertThat(cookie.getValue()).isEmpty();
    }
}
