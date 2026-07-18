package com.smartrecycle.backend.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserSettingsRequest(
        @NotBlank(message = "알림 동의 여부는 필수입니다.")
        Boolean notificationEnabled,
        @NotBlank(message = "위치 이용 동의 여부는 필수입니다.")
        Boolean locationEnabled
) {
}
