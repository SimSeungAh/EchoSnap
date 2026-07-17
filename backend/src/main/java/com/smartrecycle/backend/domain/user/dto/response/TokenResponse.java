package com.smartrecycle.backend.domain.user.dto.response;

public record TokenResponse(
    String accessToken,
    String refreshToken
) {
}