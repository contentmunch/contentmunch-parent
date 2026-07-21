package com.contentmunch.authentication.service;

import com.contentmunch.authentication.config.AuthConfigProperties;
import com.contentmunch.authentication.model.ContentmunchRole;
import com.contentmunch.authentication.model.ContentmunchUser;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

@Service
public class TokenizationService {

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_NAME = "name";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_ORG_ID = "organizationId";
    private static final String CLAIM_ENABLED = "enabled";
    private static final String CLAIM_TYPE = "type";
    private final AuthConfigProperties authConfig;
    private SecretKey secretKey;

    public TokenizationService(AuthConfigProperties authConfig) {
        this.authConfig = authConfig;
    }

    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(authConfig.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(final ContentmunchUser user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getUsername())
                .claim(CLAIM_EMAIL, user.email())
                .claim(CLAIM_NAME, user.name())
                .claim(CLAIM_USER_ID, user.id())
                .claim(CLAIM_ORG_ID, user.organizationId())
                .claim(CLAIM_ENABLED, user.enabled())
                .claim(CLAIM_TYPE, "access")
                .claim(
                        CLAIM_ROLES,
                        user.getAuthorities().stream()
                                .map(GrantedAuthority::getAuthority)
                                .toList())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(authConfig.accessTokenMaxAgeInMinutes(), ChronoUnit.MINUTES)))
                .signWith(secretKey)
                .compact();
    }

    public String generateRefreshToken(final ContentmunchUser user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("type", "refresh")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(authConfig.refreshTokenMaxAgeDays(), ChronoUnit.DAYS)))
                .signWith(secretKey)
                .compact();
    }

    private static final long API_CLIENT_TOKEN_MAX_AGE_DAYS = 365;

    public String generateApiClientToken(final ContentmunchUser user) {
        var authorities = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        if (!authorities.equals(Set.of(ContentmunchRole.ROLE_API_CLIENT.name()))) {
            throw new IllegalStateException("Refusing to mint API-client token for user '" + user.getUsername()
                    + "' -- user must have exactly ROLE_API_CLIENT and nothing else, has: " + authorities);
        }

        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getUsername())
                .claim(CLAIM_EMAIL, user.email())
                .claim(CLAIM_NAME, user.name())
                .claim(CLAIM_USER_ID, user.id())
                .claim(CLAIM_ORG_ID, user.organizationId())
                .claim(CLAIM_ENABLED, user.enabled())
                .claim(CLAIM_TYPE, "api_client")
                // Hardcoded, not user.getAuthorities() -- the token's role is
                // exactly ROLE_API_CLIENT by construction, independent of what's
                // actually stored on the user row. The guard above already
                // requires the row to match this, but the claim itself doesn't
                // rely on that guard holding forever; it's self-contained.
                .claim(CLAIM_ROLES, List.of(ContentmunchRole.ROLE_API_CLIENT.name()))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(API_CLIENT_TOKEN_MAX_AGE_DAYS, ChronoUnit.DAYS)))
                .signWith(secretKey)
                .compact();
    }

    public boolean validateToken(final String token) {
        try {
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {

            return false;
        }
    }

    public String extractUsername(final String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public ContentmunchUser extractUser(final String token) {
        var payload = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        String username = payload.getSubject();
        String email = payload.get(CLAIM_EMAIL, String.class);
        String name = payload.get(CLAIM_NAME, String.class);
        Long userId = payload.get(CLAIM_USER_ID, Long.class);
        Long organizationId = payload.get(CLAIM_ORG_ID, Long.class);
        Boolean enabled = payload.get(CLAIM_ENABLED, Boolean.class);

        // Extract roles as a List<String> and map to authorities
        @SuppressWarnings("unchecked")
        List<String> roles = payload.get(CLAIM_ROLES, List.class);

        Set<ContentmunchRole> authorities = roles.stream()
                .map(s -> ContentmunchRole.valueOf(s.trim().toUpperCase()))
                .collect(Collectors.toSet());

        return ContentmunchUser.builder()
                .id(userId)
                .enabled(enabled)
                .username(username)
                .organizationId(organizationId)
                .email(email)
                .name(name)
                .roles(authorities)
                .build();
    }
}
