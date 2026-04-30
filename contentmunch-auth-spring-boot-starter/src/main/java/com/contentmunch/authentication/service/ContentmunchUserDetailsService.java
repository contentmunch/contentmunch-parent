package com.contentmunch.authentication.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.contentmunch.authentication.model.ContentmunchUser;

public abstract class ContentmunchUserDetailsService implements UserDetailsService {

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException{
        ContentmunchUser user = loadContentmunchUser(username);
        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }
        return user;
    }

    protected abstract ContentmunchUser loadContentmunchUser(String username);
}
