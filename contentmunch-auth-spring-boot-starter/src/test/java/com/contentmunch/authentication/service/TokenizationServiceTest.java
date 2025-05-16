package com.contentmunch.authentication.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.contentmunch.authentication.config.AuthConfigProperties;
import com.contentmunch.authentication.model.ContentmunchRole;
import com.contentmunch.authentication.model.ContentmunchUser;

import io.jsonwebtoken.security.Keys;

class TokenizationServiceTest {

    private TokenizationService tokenizationService;
    private ContentmunchUser user;

    @BeforeEach
    void setUp(){
        String secret = "a-very-secure-secret-key-12345678901234567890"; // 32+ chars for HS256
        AuthConfigProperties authConfig = AuthConfigProperties.builder().maxAgeInMinutes(60).secret(secret)
                .cookie(AuthConfigProperties.CookieConfig.builder().name("token")
                        .sameSite(AuthConfigProperties.CookieConfig.SameSite.LAX).secure(true).httpOnly(true).path("/")
                        .build())
                .users(Map.of()).build();

        tokenizationService = new TokenizationService(authConfig);
        tokenizationService.init();
        user = ContentmunchUser.builder().name("John Doe").username("user1").email("john@example.com").password("password")
                .roles(Set.of(ContentmunchRole.ROLE_USER)).build();
    }

    @Test
    void shouldGenerateAndValidateTokenSuccessfully(){
        String token = tokenizationService.generateToken(user);

        assertThat(token).isNotNull();
        assertThat(tokenizationService.validateToken(token)).isTrue();
    }

    @Test
    void shouldExtractUsernameFromToken(){
        String token = tokenizationService.generateToken(user);

        String extractedUsername = tokenizationService.extractUsername(token);

        assertThat(extractedUsername).isEqualTo("user1");
    }

    @Test
    void shouldFailValidationForInvalidToken(){
        String invalidToken = "this.is.not.valid";

        assertThat(tokenizationService.validateToken(invalidToken)).isFalse();
    }

    @Test
    void shouldThrowExceptionWhenExtractingFromInvalidToken(){
        String invalidToken = "invalid.token.value";

        assertThrows(Exception.class,() -> tokenizationService.extractUsername(invalidToken));
    }

    @Test
    void shouldGenerateTokenWithExpiration(){
        String token = tokenizationService.generateToken(user);

        var claims = io.jsonwebtoken.Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor("a-very-secure-secret-key-12345678901234567890".getBytes())).build()
                .parseSignedClaims(token).getPayload();

        assertThat(claims.getExpiration().toInstant()).isAfter(Instant.now());
    }

    @Test
    void shouldFailValidationForExpiredToken(){
        var expiredAuthConfig = AuthConfigProperties.builder().maxAgeInMinutes(-1) // already expired
                .secret("a-very-secure-secret-key-12345678901234567890")
                .cookie(AuthConfigProperties.CookieConfig.builder().name("token")
                        .sameSite(AuthConfigProperties.CookieConfig.SameSite.LAX).secure(true).httpOnly(true).path("/")
                        .build())
                .users(Map.of()).build();

        TokenizationService expiredTokenService = new TokenizationService(expiredAuthConfig);
        expiredTokenService.init();
        String token = expiredTokenService.generateToken(user);

        assertThat(expiredTokenService.validateToken(token)).isFalse();
    }

    @Test
    void shouldFailValidationForTamperedToken(){
        String validToken = tokenizationService.generateToken(user);
        String tamperedToken = validToken.substring(0,validToken.length() - 1) + "x"; // change last char

        assertThat(tokenizationService.validateToken(tamperedToken)).isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldContainCorrectClaimsInToken(){
        String token = tokenizationService.generateToken(user);

        var claims = io.jsonwebtoken.Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor("a-very-secure-secret-key-12345678901234567890".getBytes())).build()
                .parseSignedClaims(token).getPayload();

        assertThat(claims.get("email",String.class)).isEqualTo("john@example.com");
        assertThat(claims.get("name",String.class)).isEqualTo("John Doe");
        assertThat((List<String>) claims.get("roles")).contains("ROLE_USER");
    }
}
