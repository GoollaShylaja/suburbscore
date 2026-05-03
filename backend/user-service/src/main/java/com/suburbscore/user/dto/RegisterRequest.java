package com.suburbscore.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload for registering a new user")
public record RegisterRequest(
        @NotBlank @Email
        @Schema(example = "john.smith@example.com", description = "Valid email address")
        String email,

        @NotBlank @Size(min = 8, message = "Password must be at least 8 characters")
        @Schema(example = "securePass123", description = "Minimum 8 characters")
        String password,

        @NotBlank
        @Schema(example = "John")
        String firstName,

        @NotBlank
        @Schema(example = "Smith")
        String lastName
) {}
