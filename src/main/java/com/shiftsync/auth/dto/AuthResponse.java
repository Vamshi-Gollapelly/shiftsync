package com.shiftsync.auth.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        long expiresInSeconds,
        String role,
        String businessSlug
) {}