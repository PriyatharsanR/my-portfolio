package com.backend.portfolio.service;

import com.backend.portfolio.entity.Admin;
import com.backend.portfolio.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final AdminRepository adminRepository;


    @Override
    public AdminUserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Admin admin = adminRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
        if (!StringUtils.hasText(admin.getUsername()) || !StringUtils.hasText(admin.getPassword())
                || !StringUtils.hasText(admin.getRole()) || !admin.getRole().matches("ROLE_[A-Z][A-Z0-9_]*")
                || "ROLE_DISABLED".equals(admin.getRole()) || admin.getVersion() == null
                || admin.getVersion() < 0) {
            throw new UsernameNotFoundException("Invalid credentials");
        }
        return new AdminUserDetails(admin.getUsername(), admin.getPassword(), admin.getRole(), admin.getVersion());
    }
}
