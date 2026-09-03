package com.echosnap.backend.domain.user.dto.response;

public record TokenResponse(
    String accessToken,
    String refreshToken
) {
}