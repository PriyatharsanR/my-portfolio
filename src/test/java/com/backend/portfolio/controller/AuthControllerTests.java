package com.backend.portfolio.controller;

import com.backend.portfolio.config.ApiSecurityErrors;
import com.backend.portfolio.service.AuthService;
import com.backend.portfolio.service.LoginAttemptLimiter;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class AuthControllerTests {

    private final AuthService service = mock(AuthService.class);
    private final MockMvc mvc = standaloneSetup(new AuthController(service))
            .setControllerAdvice(new AuthExceptionHandler(new ApiSecurityErrors(new com.fasterxml.jackson.databind.ObjectMapper())))
            .build();

    @Test
    void badCredentialsReturnSafeJsonWithoutExceptionDetails() throws Exception {
        when(service.login(any(), any())).thenThrow(new BadCredentialsException("private account detail"));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"owner\",\"password\":\"test-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("private"))));
    }

    @Test
    void rateLimitsIncludeRetryAfter() throws Exception {
        when(service.login(any(), any())).thenThrow(new LoginAttemptLimiter.LimitExceeded(30));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"owner\",\"password\":\"test-password\"}"))
                .andExpect(status().isTooManyRequests()).andExpect(header().string("Retry-After", "30"));
    }

    @Test
    void unavailableAuthenticationReturns503() throws Exception {
        when(service.login(any(), any())).thenThrow(new InternalAuthenticationServiceException("private database"));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"owner\",\"password\":\"test-password\"}"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void invalidInputDoesNotEchoPasswords() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"test-password\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.username").exists())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("test-password"))));
        verifyNoInteractions(service);
    }
}
