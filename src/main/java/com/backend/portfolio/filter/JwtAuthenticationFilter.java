package com.backend.portfolio.filter;

import com.backend.portfolio.service.CustomUserDetailsService;
import com.backend.portfolio.service.AdminUserDetails;
import com.backend.portfolio.config.ApiSecurityErrors;
import com.backend.portfolio.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.dao.DataAccessException;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final ApiSecurityErrors securityErrors;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.regionMatches(true, 0, "Bearer ", 0, 7)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                JwtUtil.TokenIdentity identity = jwtUtil.parseToken(authHeader.substring(7));
                AdminUserDetails userDetails = userDetailsService.loadUserByUsername(identity.username());
                if (!identity.username().equals(userDetails.getUsername())
                        || identity.version() != userDetails.getVersion() || !userDetails.isEnabled()
                        || !userDetails.isAccountNonLocked() || !userDetails.isAccountNonExpired()
                        || !userDetails.isCredentialsNonExpired()) {
                    throw new BadCredentialsException("Invalid credentials");
                }
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities() );

                auth.setDetails( new WebAuthenticationDetailsSource().buildDetails(request) );

                SecurityContextHolder.getContext().setAuthentication(auth);

            } catch (JwtException | IllegalArgumentException | AuthenticationException exception) {
                SecurityContextHolder.clearContext();
                securityErrors.commence(request, response, new BadCredentialsException("Invalid credentials"));
                return;
            } catch (DataAccessException exception) {
                SecurityContextHolder.clearContext();
                securityErrors.write(response, 503, "Service Unavailable", "Authentication is temporarily unavailable.", Map.of());
                return;
            }
        }

        filterChain.doFilter(request,response);
    }
}
