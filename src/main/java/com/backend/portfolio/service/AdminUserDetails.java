package com.backend.portfolio.service;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.List;

public class AdminUserDetails extends User {

    private final long version;

    public AdminUserDetails(String username, String password, String role, long version) {
        super(username, password, List.of(new SimpleGrantedAuthority(role)));
        this.version = version;
    }

    public long getVersion() {
        return version;
    }
}
