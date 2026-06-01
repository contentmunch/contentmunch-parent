package com.contentmunch.authentication.service;

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

import com.contentmunch.authentication.config.AuthConfigProperties;
import com.contentmunch.authentication.model.ContentmunchRole;
import com.contentmunch.authentication.model.ContentmunchUser;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;

@Service
public class TokenizationService {

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_NAME = "name";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_ENABLED = "enabled";
    private static final String CLAIM_TYPE = "type";
    private final AuthConfigProperties authConfig;
    private SecretKey secretKey;

    public TokenizationService(AuthConfigProperties authConfig) {
        this.authConfig = authConfig;

    }

    @PostConstruct
    public void init(){
        this.secretKey = Keys.hmacShaKeyFor(authConfig.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(final ContentmunchUser user){
        Instant now = Instant.now();
        return Jwts.builder().subject(user.getUsername()).claim(CLAIM_EMAIL,user.email()).claim(CLAIM_NAME,user.name())
                .claim(CLAIM_USER_ID,user.id()).claim(CLAIM_ENABLED,user.enabled()).claim(CLAIM_TYPE,"access")
                .claim(CLAIM_ROLES,user.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(authConfig.accessTokenMaxAgeInMinutes(),ChronoUnit.MINUTES)))
                .signWith(secretKey).compact();
    }

    public String generateRefreshToken(final ContentmunchUser user){
        Instant now = Instant.now();
        return Jwts.builder().subject(user.getUsername()).claim("type","refresh").issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(authConfig.refreshTokenMaxAgeDays(),ChronoUnit.DAYS)))
                .signWith(secretKey).compact();
    }

    public String generatePermanentAccessToken(final ContentmunchUser user){
        Instant now = Instant.now();
        // 100 years in minutes: 100 * 365.25 * 24 * 60
        long oneHundredYearsInMinutes = 52596000L;

        return Jwts.builder().subject(user.getUsername()).claim(CLAIM_EMAIL,user.email()).claim(CLAIM_NAME,user.name())
                .claim(CLAIM_USER_ID,user.id()).claim(CLAIM_ENABLED,user.enabled())
                .claim(CLAIM_ROLES,user.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList())
                // Custom claim to identify permanent keys if you need to trace them later
                .claim(CLAIM_TYPE,"permanent").issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(oneHundredYearsInMinutes,ChronoUnit.MINUTES))).signWith(secretKey)
                .compact();
    }

    public boolean validateToken(final String token){
        try {
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {

            return false;
        }
    }

    public String extractUsername(final String token){
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().getSubject();
    }

    public ContentmunchUser extractUser(final String token){
        var payload = Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
        String username = payload.getSubject();
        String email = payload.get(CLAIM_EMAIL,String.class);
        String name = payload.get(CLAIM_NAME,String.class);
        Long userId = payload.get(CLAIM_USER_ID,Long.class);
        Boolean enabled = payload.get(CLAIM_ENABLED,Boolean.class);

        // Extract roles as a List<String> and map to authorities
        @SuppressWarnings("unchecked")
        List<String> roles = payload.get(CLAIM_ROLES,List.class);

        Set<ContentmunchRole> authorities = roles.stream().map(s -> ContentmunchRole.valueOf(s.trim().toUpperCase()))
                .collect(Collectors.toSet());

        return ContentmunchUser.builder().id(userId).enabled(enabled).username(username).email(email).name(name)
                .roles(authorities).build();
    }
}
