package com.backend.portfolio.controller;

import com.backend.portfolio.dto.LoginRequest;
import com.backend.portfolio.dto.LoginResponse;
import com.backend.portfolio.service.AuthService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest){
        return ResponseEntity.ok().header("Cache-Control", "no-store")
                .body(authService.login(request, httpRequest.getRemoteAddr()));
    }
}
