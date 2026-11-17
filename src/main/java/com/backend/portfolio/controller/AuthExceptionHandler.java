package com.backend.portfolio.controller;

import com.backend.portfolio.config.ApiSecurityErrors;
import com.backend.portfolio.service.LoginAttemptLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(assignableTypes = AuthController.class)
@RequiredArgsConstructor
public class AuthExceptionHandler {

    private final ApiSecurityErrors securityErrors;

    @ExceptionHandler(InternalAuthenticationServiceException.class)
    public void authenticationUnavailable(HttpServletResponse response) throws IOException {
        securityErrors.write(response, 503, "Service Unavailable", "Authentication is temporarily unavailable.", Map.of());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public void malformedRequest(HttpServletResponse response) throws IOException {
        securityErrors.write(response, 400, "Bad Request", "The login request is invalid.", Map.of());
    }

    @ExceptionHandler(AuthenticationException.class)
    public void authenticationFailed(AuthenticationException exception, HttpServletRequest request,
                                     HttpServletResponse response) throws IOException {
        securityErrors.commence(request, response, exception);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public void invalidInput(MethodArgumentNotValidException exception, HttpServletResponse response)
            throws IOException {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        securityErrors.write(response, 400, "Bad Request", "The login request is invalid.", Map.of("errors", errors));
    }

    @ExceptionHandler(LoginAttemptLimiter.LimitExceeded.class)
    public void throttled(LoginAttemptLimiter.LimitExceeded exception, HttpServletResponse response)
            throws IOException {
        response.setHeader("Retry-After", Long.toString(exception.getRetryAfter()));
        securityErrors.write(response, 429, "Too Many Requests", "Try signing in again later.", Map.of());
    }
}
