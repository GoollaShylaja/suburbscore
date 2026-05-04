package com.suburbscore.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload for registering a new user")
public record RegisterRequest(
        @NotBlank @Email
        @Schema(example = "john.smith@example.com", description = "Valid email address")
        String email,

        @NotBlank
        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).*$",
                 message = "Password must contain at least one letter and one number")
        @Schema(example = "securePass123", description = "8–72 characters, must include a letter and a number")
        String password,

        @NotBlank
        @Schema(example = "John")
        String firstName,

        @NotBlank
        @Schema(example = "Smith")
        String lastName
) {}
