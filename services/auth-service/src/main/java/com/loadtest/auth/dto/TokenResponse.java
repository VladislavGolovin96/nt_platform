package com.loadtest.auth.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken
) {}
