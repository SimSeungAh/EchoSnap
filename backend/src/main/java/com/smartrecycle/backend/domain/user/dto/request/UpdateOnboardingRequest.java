package com.smartrecycle.backend.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateOnboardingRequest(
        @NotBlank(message = "초기 설정 완료 여부는 필수입니다.")
        Boolean completed
) {
}
