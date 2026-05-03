package com.suburbscore.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request payload for user login")
public record LoginRequest(
        @NotBlank @Email
        @Schema(example = "john.smith@example.com")
        String email,

        @NotBlank
        @Schema(example = "securePass123")
        String password
) {}
