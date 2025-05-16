package com.contentmunch.authentication.model;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Builder;

@Builder
public record ContentmunchUser(String name, String email, Set<ContentmunchRole> roles, String username,
        @JsonIgnore String password) implements UserDetails {

    public ContentmunchUser {
        roles = roles == null ? Set.of() : Set.copyOf(roles);
    }

    public static class ContentmunchUserBuilder {
        public ContentmunchUserBuilder roles(Set<ContentmunchRole> roles){
            this.roles = roles == null ? Set.of() : Set.copyOf(roles);
            return this;
        }
    }

    @Override
    @JsonIgnore
    public Set<? extends GrantedAuthority> getAuthorities(){
        return roles.stream().map(role -> new SimpleGrantedAuthority(role.name())).collect(Collectors.toSet());
    }

    @Override
    public String getUsername(){
        return username;
    }

    @Override
    public String getPassword(){
        return password;
    }

    @Override
    public boolean isAccountNonExpired(){
        return true;
    }

    @Override
    public boolean isAccountNonLocked(){
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired(){
        return true;
    }

    @Override
    public boolean isEnabled(){
        return true;
    }

}
