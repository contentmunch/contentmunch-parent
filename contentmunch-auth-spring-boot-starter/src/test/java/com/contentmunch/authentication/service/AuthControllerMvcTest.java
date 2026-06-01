package com.contentmunch.authentication.service;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.Set;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.contentmunch.authentication.controller.AuthController;
import com.contentmunch.authentication.error.SecurityExceptionHandler;
import com.contentmunch.authentication.model.ContentmunchRole;
import com.contentmunch.authentication.model.ContentmunchUser;
import com.contentmunch.foundation.error.GlobalExceptionHandler;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, SecurityExceptionHandler.class})
class AuthControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    // Use @Autowired now, as the @TestConfiguration provides the bean
    @Autowired
    private ContentmunchUserDetailsService userDetailsService;

    @MockitoBean
    private CookieService cookieService;

    @MockitoBean
    private TokenizationService tokenizationService;

    // This puts your partial mock into the Spring Context
    @TestConfiguration
    static class TestConfig {
        @Bean
        public ContentmunchUserDetailsService userDetailsService() {
            return mock(ContentmunchUserDetailsService.class, CALLS_REAL_METHODS);
        }
    }

    private final ContentmunchUser contentmunchUser = ContentmunchUser.builder()
            .id(1L)
            .enabled(true)
            .username("user")
            .password("{noop}pass}")
            .email("user@email.com")
            .name("User Name")
            .roles(Set.of(ContentmunchRole.ROLE_USER, ContentmunchRole.ROLE_ADMIN))
            .build();

    @Test
    void login_shouldAuthenticateAndSetCookieAndReturnUser() throws Exception {
        String jwt = "fake-jwt-token";
        var expectedCookie = ResponseCookie.from("contentmunch-auth", jwt).build();
        String jsonBody =
                """
                {
                  "username": "user",
                  "password": "pass"
                }
                """;

        // 1. Stub the UserDetailsService (Already done)
        doReturn(contentmunchUser).when(userDetailsService).loadContentmunchUser("user");

        // 2. STUB THE AUTHENTICATION MANAGER (The Missing Piece)
        // Create an Authentication object that holds your user
        Authentication auth =
                new UsernamePasswordAuthenticationToken(contentmunchUser, null, contentmunchUser.getAuthorities());

        // Tell the mock manager to return this auth object
        when(authenticationManager.authenticate(any())).thenReturn(auth);

        // 3. Stub the rest (Already done)
        when(tokenizationService.generateAccessToken(contentmunchUser)).thenReturn(jwt);
        when(tokenizationService.generateRefreshToken(contentmunchUser)).thenReturn(jwt);
        when(cookieService.cookieFromAccessToken(jwt)).thenReturn(expectedCookie);
        when(cookieService.cookieFromRefreshToken(jwt)).thenReturn(expectedCookie);

        // 4. Execute
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("user"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("contentmunch-auth=" + jwt)));
    }

    @Test
    void logout_shouldClearAuthCookie() throws Exception {
        var logoutCookie =
                ResponseCookie.from("contentmunch-auth", "").maxAge(0).build();

        when(cookieService.cookieFromAccessToken("", 0)).thenReturn(logoutCookie);

        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(content().string("Logged out"))
                .andExpect(header().string(
                                HttpHeaders.SET_COOKIE,
                                Matchers.allOf(
                                        Matchers.containsString("contentmunch-auth="),
                                        Matchers.containsString("Max-Age=0"),
                                        Matchers.containsString("Expires="))));
    }

    @Test
    void getProtected_shouldReturnUserFromSecurityContext() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken(contentmunchUser, null, contentmunchUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("user"));
    }

    @Test
    void getProtected_shouldReturnAccessDenied_ifPrincipalIsInvalid() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("not-a-user", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }
}
