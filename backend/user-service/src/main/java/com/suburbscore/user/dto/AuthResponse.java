package com.suburbscore.user.dto;

public record AuthResponse(
        String token,
        String tokenType,
        long expiresIn,
        String userId,
        String email,
        String firstName,
        String lastName
) {}
