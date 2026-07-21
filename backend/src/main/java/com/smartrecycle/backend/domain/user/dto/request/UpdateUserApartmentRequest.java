package com.smartrecycle.backend.domain.user.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateUserApartmentRequest(

        @NotNull(message = "아파트 ID는 필수입니다.")
        Long apartmentId

) {
}