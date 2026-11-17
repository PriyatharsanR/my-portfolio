package com.backend.portfolio.service;
import com.backend.portfolio.dto.LoginRequest;
import com.backend.portfolio.dto.LoginResponse;
import com.backend.portfolio.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final LoginAttemptLimiter loginAttemptLimiter;

    public LoginResponse login(LoginRequest request, String clientAddress) {
        loginAttemptLimiter.check(clientAddress);
        if (request == null || request.getPassword() == null
                || request.getPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BadCredentialsException("Invalid credentials");
        }
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        if (!authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof AdminUserDetails owner)) {
            throw new BadCredentialsException("Invalid credentials");
        }
        String role = authentication.getAuthorities().stream().map(authority -> authority.getAuthority())
                .filter(authority -> authority.startsWith("ROLE_"))
                .findFirst().orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        String token = jwtUtil.generateToken(owner.getUsername(), owner.getVersion());

        return LoginResponse.builder()
                .token(token)
                .username(owner.getUsername())
                .role(role)
                .build();
    }
}
