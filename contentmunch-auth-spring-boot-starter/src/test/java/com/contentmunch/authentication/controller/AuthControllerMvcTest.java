package com.contentmunch.authentication.controller;

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
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.contentmunch.authentication.model.ContentmunchRole;
import com.contentmunch.authentication.model.ContentmunchUser;
import com.contentmunch.authentication.service.CookieService;
import com.contentmunch.authentication.service.TokenizationService;
import com.contentmunch.foundation.error.GlobalExceptionHandler;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private CookieService cookieService;

    @MockitoBean
    private TokenizationService tokenizationService;

    private final ContentmunchUser contentmunchUser = ContentmunchUser.builder().username("user")
            .password("{noop}pass}").email("user@email.com").name("User Name")
            .roles(Set.of(ContentmunchRole.ROLE_USER,ContentmunchRole.ROLE_ADMIN)).build();

    @Test
    void login_shouldAuthenticateAndSetCookieAndReturnUser() throws Exception{
        String jwt = "fake-jwt-token";
        var expectedCookie = ResponseCookie.from("contentmunch-auth",jwt).build();
        String jsonBody = """
                {
                  "username": "user",
                  "password": "pass"
                }
                """;

        when(userDetailsService.loadUserByUsername("user")).thenReturn(contentmunchUser);
        when(tokenizationService.generateAccessToken(contentmunchUser)).thenReturn(jwt);
        when(tokenizationService.generateRefreshToken(contentmunchUser)).thenReturn(jwt);
        when(cookieService.cookieFromAccessToken(jwt)).thenReturn(expectedCookie);
        when(cookieService.cookieFromRefreshToken(jwt)).thenReturn(expectedCookie);

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(jsonBody))
                .andExpect(status().isOk()).andExpect(jsonPath("$.username").value("user"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE,containsString("contentmunch-auth=" + jwt)));

        verify(authenticationManager).authenticate(any());
    }

    @Test
    void login_shouldReturnAccessDenied_whenUserIsNotContentmunchUser() throws Exception{
        String jsonBody = """
                {
                  "username": "user",
                  "password": "pass"
                }
                """;

        when(userDetailsService.loadUserByUsername("user")).thenReturn(mock(UserDetails.class));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(jsonBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_shouldClearAuthCookie() throws Exception{
        var logoutCookie = ResponseCookie.from("contentmunch-auth","").maxAge(0).build();

        when(cookieService.cookieFromAccessToken("",0)).thenReturn(logoutCookie);

        mockMvc.perform(post("/api/auth/logout")).andExpect(status().isOk()).andExpect(content().string("Logged out"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE,
                        Matchers.allOf(Matchers.containsString("contentmunch-auth="),
                                Matchers.containsString("Max-Age=0"),Matchers.containsString("Expires="))));
    }

    @Test
    void getProtected_shouldReturnUserFromSecurityContext() throws Exception{
        var auth = new UsernamePasswordAuthenticationToken(contentmunchUser, null, contentmunchUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(get("/api/auth/me")).andExpect(status().isOk()).andExpect(jsonPath("$.username").value("user"));
    }

    @Test
    void getProtected_shouldReturnAccessDenied_ifPrincipalIsInvalid() throws Exception{
        var auth = new UsernamePasswordAuthenticationToken("not-a-user", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }
}
