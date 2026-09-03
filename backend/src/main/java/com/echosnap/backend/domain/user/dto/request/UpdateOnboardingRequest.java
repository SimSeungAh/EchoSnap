package com.echosnap.backend.domain.user.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateOnboardingRequest(

    @NotNull(message = "초기 설정 완료 여부는 필수입니다.")
    Boolean completed

) {
}