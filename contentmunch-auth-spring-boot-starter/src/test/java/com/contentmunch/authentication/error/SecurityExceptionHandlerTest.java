package com.contentmunch.authentication.error;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.contentmunch.authentication.controller.AuthController;
import com.contentmunch.authentication.service.CookieService;
import com.contentmunch.authentication.service.TokenizationService;
import com.contentmunch.foundation.error.GlobalExceptionHandler;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class SecurityExceptionHandlerTest {

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

    private static final String LOGIN_ENDPOINT = "/api/auth/login";

    @Test
    void shouldHandleBadCredentialsException() throws Exception{
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Invalid credentials"));

        performLogin().andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorMessage").value("Invalid username or password"))
                .andExpect(jsonPath("$.errorCode").value("BAD_CREDENTIALS"));
    }

    @Test
    void shouldHandleUsernameNotFoundException() throws Exception{
        when(authenticationManager.authenticate(any())).thenThrow(new UsernameNotFoundException("User not found"));

        performLogin().andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorMessage").value("Username not found"))
                .andExpect(jsonPath("$.errorCode").value("USERNAME_NOT_FOUND"));
    }

    @Test
    void shouldHandleCredentialsNotFoundException() throws Exception{
        when(authenticationManager.authenticate(any()))
                .thenThrow(new AuthenticationCredentialsNotFoundException("Missing credentials"));

        performLogin().andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorMessage").value("Authentication credentials not found"))
                .andExpect(jsonPath("$.errorCode").value("CREDENTIALS_MISSING"));
    }

    @Test
    void shouldHandleDisabledException() throws Exception{
        when(authenticationManager.authenticate(any())).thenThrow(new DisabledException("Account disabled"));

        performLogin().andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorMessage").value("Your account is disabled"))
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_DISABLED"));
    }

    @Test
    void shouldHandleLockedException() throws Exception{
        when(authenticationManager.authenticate(any())).thenThrow(new LockedException("Account locked"));

        performLogin().andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorMessage").value("Your account is locked"))
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_LOCKED"));
    }

    @Test
    void shouldHandleCredentialsExpiredException() throws Exception{
        when(authenticationManager.authenticate(any()))
                .thenThrow(new CredentialsExpiredException("Credentials expired"));

        performLogin().andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorMessage").value("Your credentials have expired"))
                .andExpect(jsonPath("$.errorCode").value("CREDENTIALS_EXPIRED"));
    }

    @Test
    void shouldHandleAccessDeniedException() throws Exception{
        when(authenticationManager.authenticate(any())).thenThrow(new AccessDeniedException("Access denied"));

        performLogin().andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorMessage").value("You do not have permission"))
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }

    @Test
    void shouldHandleAuthenticationServiceException() throws Exception{
        when(authenticationManager.authenticate(any())).thenThrow(new AuthenticationServiceException("Internal error"));

        performLogin().andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorMessage").value("Authentication service error"))
                .andExpect(jsonPath("$.errorCode").value("AUTH_SERVICE_ERROR"));
    }

    @Test
    void shouldHandleInsufficientAuthenticationException() throws Exception{
        when(authenticationManager.authenticate(any()))
                .thenThrow(new InsufficientAuthenticationException("Insufficient auth"));

        performLogin().andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorMessage").value("Insufficient authentication"))
                .andExpect(jsonPath("$.errorCode").value("INSUFFICIENT_AUTH"));
    }

    private ResultActions performLogin() throws Exception{
        String jsonBody = """
                {
                  "username": "user",
                  "password": "pass"
                }
                """;
        return mockMvc.perform(
                MockMvcRequestBuilders.post(LOGIN_ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(jsonBody));
    }
}
