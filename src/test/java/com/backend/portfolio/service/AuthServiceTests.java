package com.backend.portfolio.service;

import com.backend.portfolio.dto.LoginRequest;
import com.backend.portfolio.dto.LoginResponse;
import com.backend.portfolio.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTests {

    @Test
    void tokenAndResponseUseTheAuthenticatedIdentityAndActualRole() {
        AuthenticationManager manager = mock(AuthenticationManager.class);
        JwtUtil jwt = mock(JwtUtil.class);
        LoginAttemptLimiter limiter = mock(LoginAttemptLimiter.class);
        AdminUserDetails principal = new AdminUserDetails("CanonicalOwner", "encoded", "ROLE_USER", 3);
        when(manager.authenticate(any())).thenReturn(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        when(jwt.generateToken("CanonicalOwner", 3)).thenReturn("test-token");
        LoginRequest request = new LoginRequest();
        request.setUsername("canonicalowner");
        request.setPassword("test-password");

        LoginResponse response = new AuthService(manager, jwt, limiter).login(request, "127.0.0.1");

        assertThat(response.getUsername()).isEqualTo("CanonicalOwner");
        assertThat(response.getRole()).isEqualTo("ROLE_USER");
        assertThat(response.getToken()).isEqualTo("test-token");
        verify(jwt).generateToken("CanonicalOwner", 3);
        verify(limiter).check("127.0.0.1");
    }

    @Test
    void oversizedUtf8PasswordsAreRejectedBeforePasswordHashing() {
        AuthenticationManager manager = mock(AuthenticationManager.class);
        JwtUtil jwt = mock(JwtUtil.class);
        LoginAttemptLimiter limiter = mock(LoginAttemptLimiter.class);
        LoginRequest request = new LoginRequest();
        request.setUsername("owner");
        request.setPassword("\u00e9".repeat(40));

        assertThatThrownBy(() -> new AuthService(manager, jwt, limiter).login(request, "127.0.0.1"))
                .isInstanceOf(BadCredentialsException.class);
        verifyNoInteractions(manager, jwt);
    }
}
