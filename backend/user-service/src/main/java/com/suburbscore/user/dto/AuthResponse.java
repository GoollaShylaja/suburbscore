package com.suburbscore.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Authentication response returned on register and login")
public record AuthResponse(
        @Schema(example = "eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJqb2huLnNtaXRoQGV4YW1wbGUuY29tIn0.xxx")
        String token,

        @Schema(example = "Bearer")
        String tokenType,

        @Schema(example = "86400", description = "Token validity in seconds")
        long expiresIn,

        @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        String userId,

        @Schema(example = "john.smith@example.com")
        String email,

        @Schema(example = "John")
        String firstName,

        @Schema(example = "Smith")
        String lastName
) {}
