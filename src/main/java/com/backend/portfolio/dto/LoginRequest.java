package com.backend.portfolio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @NotBlank(message = "Username is required")
    @Size(max = 255, message = "Username is too long")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(max = 72, message = "Password is too long")
    private String password;
}
