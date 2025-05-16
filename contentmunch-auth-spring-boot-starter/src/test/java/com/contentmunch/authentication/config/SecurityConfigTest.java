package com.contentmunch.authentication.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.contentmunch.authentication.model.ContentmunchRole;
import com.contentmunch.authentication.model.ContentmunchUser;
import com.contentmunch.authentication.service.TokenizationService;

@SpringBootTest(properties = {"contentmunch.auth.secret=a-very-secure-secret-key-12345678901234567890",
        "contentmunch.auth.cookie.name=token", "contentmunch.auth.cookie.same-site=LAX",
        "contentmunch.auth.cookie.secure=false", "contentmunch.auth.cookie.http-only=false",
        "contentmunch.auth.cookie.path=/", "contentmunch.auth.users.testuser.name=Contentmunch",
        "contentmunch.auth.users.testuser.username=testuser",
        "contentmunch.auth.users.testuser.email=mail@contentmunch.com",
        "contentmunch.auth.users.testuser.password={noop}password",
        "contentmunch.auth.users.testuser.roles=ROLE_USER,ROLE_ADMIN"})
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenizationService tokenizationService;

    @Test
    void loginShouldReturn200AndSetCookieHeader() throws Exception{

        MvcResult result = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                    {
                      "username": "testuser",
                      "password": "password"
                    }
                """)).andExpect(status().isOk()).andExpect(header().exists(HttpHeaders.SET_COOKIE)).andReturn();

        String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).contains("token=");
    }

    @Test
    void loginShouldReturn401WithInvalidPassword() throws Exception{

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                    {
                      "username": "testuser",
                      "password": "wrongpass"
                    }
                """)).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutShouldReturn200AndSetExpiredCookie() throws Exception{
        MvcResult result = mockMvc.perform(post("/api/auth/logout")).andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE)).andReturn();

        String cookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(cookie).contains("token=");
        assertThat(cookie).contains("Max-Age=0");
    }

    @Test
    void meShouldReturn403WithoutToken() throws Exception{
        mockMvc.perform(get("/api/auth/me")).andExpect(status().isForbidden());
    }

    @Test
    void meShouldReturnUserWithValidToken() throws Exception{
        String token = tokenizationService
                .generateToken(ContentmunchUser.builder().username("testuser").password("password").name("Contentmunch")
                        .email("mail@contentmunch.com").roles(Set.of(ContentmunchRole.ROLE_USER)).build());

        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION,"Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("mail@contentmunch.com"))
                .andExpect(jsonPath("$.name").value("Contentmunch"));
    }
}
