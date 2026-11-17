package com.backend.portfolio.service;

import com.backend.portfolio.entity.Admin;
import com.backend.portfolio.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class OwnerProvisioningService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void provision(String username, char[] password) {
        validate(username, password);
        // Serializable isolation protects the empty-table check against concurrent provisioning.
        if (adminRepository.count() != 0) {
            throw new IllegalStateException("An account already exists; provisioning will not overwrite it");
        }
        Admin admin = Admin.builder().username(username).password(encode(password)).role("ROLE_ADMIN")
                .createdAt(LocalDateTime.now(ZoneOffset.UTC)).build();
        adminRepository.saveAndFlush(admin);
    }

    @Transactional
    public void resetPassword(String username, char[] password) {
        validate(username, password);
        Admin admin = adminRepository.findByUsername(username)
                .filter(account -> "ROLE_ADMIN".equals(account.getRole()))
                .orElseThrow(() -> new IllegalStateException("No matching owner account exists"));
        admin.setPassword(encode(password));
        // Flushing advances the optimistic version; older JWTs carry the previous version.
        adminRepository.saveAndFlush(admin);
    }

    private String encode(char[] password) {
        return passwordEncoder.encode(CharBuffer.wrap(password));
    }

    private void validate(String username, char[] password) {
        if (username == null || !username.matches("[A-Za-z0-9._-]{3,64}")) {
            throw new IllegalArgumentException("Owner username must contain 3-64 letters, digits, dots, underscores or hyphens");
        }
        if (password == null || CharBuffer.wrap(password).codePoints().count() < 12
                || StandardCharsets.UTF_8.encode(CharBuffer.wrap(password)).remaining() > 72
                || CharBuffer.wrap(password).chars().allMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("Owner password must contain at least 12 characters and at most 72 UTF-8 bytes");
        }
    }
}
