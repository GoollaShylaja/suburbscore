package com.suburbscore.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Full user profile including preferences")
public record UserProfileResponse(
        @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(example = "john.smith@example.com")
        String email,

        @Schema(example = "John")
        String firstName,

        @Schema(example = "Smith")
        String lastName,

        @Schema(example = "2026-05-03T10:00:00")
        LocalDateTime createdAt,

        @Schema(description = "Null if preferences have not been saved yet")
        PreferencesResponse preferences
) {}
