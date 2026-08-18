package com.smartrecycle.backend.domain.user.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateUserSettingsRequest(

    @NotNull(message = "알림 동의 여부는 필수입니다.")
    Boolean notificationEnabled,

    @NotNull(message = "위치 이용 동의 여부는 필수입니다.")
    Boolean locationEnabled

) {
}