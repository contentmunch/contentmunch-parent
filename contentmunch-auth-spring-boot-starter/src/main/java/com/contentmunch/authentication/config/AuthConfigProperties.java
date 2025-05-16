package com.contentmunch.authentication.config;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.contentmunch.authentication.model.ContentmunchUser;

import lombok.Builder;
import lombok.Getter;

@ConfigurationProperties(prefix = "contentmunch.auth")
@Builder
public record AuthConfigProperties(int maxAgeInMinutes, String secret, CookieConfig cookie,
        Map<String, ContentmunchUser> users) {

    public AuthConfigProperties {
        users = users == null ? Map.of() : Map.copyOf(users);
    }
    public static class AuthConfigPropertiesBuilder {
        public AuthConfigPropertiesBuilder users(Map<String, ContentmunchUser> users){
            this.users = users == null ? Map.of() : Map.copyOf(users);
            return this;
        }
    }

    @Builder
    public record CookieConfig(String name, SameSite sameSite, boolean secure, boolean httpOnly, String path) {

        @Getter
        public enum SameSite {
            NONE("None"), LAX("Lax"), STRICT("Strict");
            private final String value;

            SameSite(String value) {
                this.value = value;
            }

        }
    }
}
