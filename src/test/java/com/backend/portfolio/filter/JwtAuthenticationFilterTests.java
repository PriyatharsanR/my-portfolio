package com.backend.portfolio.filter;

import com.backend.portfolio.config.ApiSecurityErrors;
import com.backend.portfolio.service.AdminUserDetails;
import com.backend.portfolio.service.CustomUserDetailsService;
import com.backend.portfolio.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTests {

    private final SecretKey key = createKey();
    private final JwtUtil jwt = createJwt(key);
    private final CustomUserDetailsService users = mock(CustomUserDetailsService.class);
    private final FilterChain chain = mock(FilterChain.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwt, users,
            new ApiSecurityErrors(new ObjectMapper()));

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "broken", "header.payload.signature"})
    void malformedTokensReturnSafe401(String token) throws Exception {
        assertRejected(token, 401);
        verifyNoInteractions(users, chain);
    }

    @Test
    void expiredWrongSignatureAndIncompleteTokensAreRejected() throws Exception {
        assertRejected(Jwts.builder().setSubject("owner").claim("ver", 0)
                .setExpiration(new Date(System.currentTimeMillis() - 60_000)).signWith(key).compact(), 401);
        assertRejected(createJwt(createKey()).generateToken("owner", 0), 401);
        assertRejected(Jwts.builder().setSubject("owner").setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(key).compact(), 401);
        assertRejected(Jwts.builder().setSubject("owner").claim("ver", 0).signWith(key).compact(), 401);
        verifyNoInteractions(users, chain);
    }

    @Test
    void deletedUsersAndOldCredentialVersionsAreRejected() throws Exception {
        String token = jwt.generateToken("owner", 0);
        when(users.loadUserByUsername("owner")).thenThrow(new UsernameNotFoundException("database detail"));
        assertRejected(token, 401);

        doReturn(new AdminUserDetails("owner", "encoded", "ROLE_ADMIN", 1))
                .when(users).loadUserByUsername("owner");
        assertRejected(token, 401);
        verifyNoInteractions(chain);
    }

    @Test
    void validIdentityUsesCurrentStoredAuthorities() throws Exception {
        when(users.loadUserByUsername("owner")).thenReturn(new AdminUserDetails("owner", "encoded", "ROLE_USER", 2));
        MockHttpServletRequest request = request(jwt.generateToken("owner", 2));

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting("authority").containsExactly("ROLE_USER");
        verify(chain).doFilter(any(), any());
    }

    @Test
    void databaseOutagesRemainDistinctFromInvalidCredentials() throws Exception {
        when(users.loadUserByUsername("owner")).thenThrow(new DataAccessResourceFailureException("private database detail"));
        assertRejected(jwt.generateToken("owner", 0), 503);
        verifyNoInteractions(chain);
    }

    @Test
    void downstreamFailuresAreNotConvertedToAuthenticationErrors() throws Exception {
        doThrow(new ServletException("downstream failure")).when(chain).doFilter(any(), any());
        assertThatThrownBy(() -> filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), chain))
                .isInstanceOf(ServletException.class).hasMessage("downstream failure");
    }

    private void assertRejected(String token, int expectedStatus) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request(token), response, chain);
        assertThat(response.getStatus()).isEqualTo(expectedStatus);
        assertThat(response.getContentType()).isEqualTo("application/problem+json");
        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
        assertThat(response.getContentAsString()).doesNotContain("private", "database detail", "signature", "stackTrace");
        if (!token.isEmpty()) {
            assertThat(response.getContentAsString()).doesNotContain(token);
        }
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private static MockHttpServletRequest request(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/probe");
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }

    private static SecretKey createKey() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Keys.hmacShaKeyFor(bytes);
    }

    private static JwtUtil createJwt(SecretKey key) {
        JwtUtil utility = new JwtUtil();
        ReflectionTestUtils.setField(utility, "secret", Base64.getEncoder().encodeToString(key.getEncoded()));
        ReflectionTestUtils.setField(utility, "expiration", 60_000L);
        utility.init();
        return utility;
    }
}
