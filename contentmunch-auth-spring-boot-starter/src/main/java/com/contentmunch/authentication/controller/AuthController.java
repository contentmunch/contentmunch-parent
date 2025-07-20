package com.contentmunch.authentication.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import com.contentmunch.authentication.model.AuthRequest;
import com.contentmunch.authentication.model.ContentmunchUser;
import com.contentmunch.authentication.service.CookieService;
import com.contentmunch.authentication.service.TokenizationService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final CookieService cookieService;
    private final TokenizationService tokenizationService;

    @PostMapping("/login")
    public ResponseEntity<ContentmunchUser> login(@RequestBody AuthRequest authRequest,HttpServletResponse response){
        authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(authRequest.username(), authRequest.password()));

        var userDetails = userDetailsService.loadUserByUsername(authRequest.username());
        if (userDetails instanceof ContentmunchUser contentmunchUser) {
            // Generate tokens
            String accessToken = tokenizationService.generateAccessToken(contentmunchUser);
            String refreshToken = tokenizationService.generateRefreshToken(contentmunchUser);

            // Create cookies
            var accessTokenCookie = cookieService.cookieFromAccessToken(accessToken); // path: /
            var refreshTokenCookie = cookieService.cookieFromRefreshToken(refreshToken); // path: /api/auth/refresh

            // Set cookies
            response.setHeader(HttpHeaders.SET_COOKIE,accessTokenCookie.toString());
            response.addHeader(HttpHeaders.SET_COOKIE,refreshTokenCookie.toString());

            log.info("User {} logged in",authRequest.username());

            return ResponseEntity.ok(contentmunchUser);
        } else {
            throw new UsernameNotFoundException("User not found with username: " + authRequest.username());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response){
        var cookie = cookieService.cookieFromAccessToken("",0).toString();
        response.setHeader(HttpHeaders.SET_COOKIE,cookie);
        return ResponseEntity.ok("Logged out");
    }

    @GetMapping("/me")
    public ResponseEntity<ContentmunchUser> getProtected(){
        var auth = SecurityContextHolder.getContext().getAuthentication();
        var principal = auth.getPrincipal();
        if (principal instanceof ContentmunchUser contentmunchUser) {
            return ResponseEntity.ok(contentmunchUser);

        } else {
            throw new UsernameNotFoundException("User not found with username: " + principal);
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<ContentmunchUser> refreshToken(HttpServletRequest request,HttpServletResponse response){
        var refreshToken = cookieService.extractRefreshToken(request);
        if (!tokenizationService.validateToken(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        var username = tokenizationService.extractUsername(refreshToken);
        var userDetails = userDetailsService.loadUserByUsername(username);
        if (userDetails instanceof ContentmunchUser contentmunchUser) {
            String newAccessToken = tokenizationService.generateAccessToken(contentmunchUser);
            var newAccessCookie = cookieService.cookieFromAccessToken(newAccessToken).toString();
            response.setHeader(HttpHeaders.SET_COOKIE,newAccessCookie);

            // Optional: rotate refresh token
            String newRefreshToken = tokenizationService.generateRefreshToken(contentmunchUser);
            var refreshCookie = cookieService.cookieFromRefreshToken(newRefreshToken).toString();
            response.addHeader(HttpHeaders.SET_COOKIE,refreshCookie);

            log.info("Access token refreshed for user {}",contentmunchUser.getUsername());

            return ResponseEntity.ok(contentmunchUser);
        } else {
            throw new UsernameNotFoundException("User not found with username: " + username);
        }
    }
}
