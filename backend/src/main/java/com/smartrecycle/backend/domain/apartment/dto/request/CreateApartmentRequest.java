package com.smartrecycle.backend.domain.apartment.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateApartmentRequest(

        @NotBlank(message = "아파트 이름은 필수입니다.")
        @Size(
                max = 100,
                message = "아파트 이름은 100자 이하여야 합니다."
        )
        String name,

        @NotBlank(message = "도로명 주소는 필수입니다.")
        @Size(
                max = 255,
                message = "도로명 주소는 255자 이하여야 합니다."
        )
        String roadAddress,

        @Size(
                max = 255,
                message = "지번 주소는 255자 이하여야 합니다."
        )
        String jibunAddress,

        @NotBlank(message = "건물관리번호는 필수입니다.")
        @Pattern(
                regexp = "\\d{25}",
                message = "건물관리번호는 25자리 숫자여야 합니다."
        )
        String buildingManagementNumber,

        @Digits(
                integer = 2,
                fraction = 7,
                message = "위도 형식이 올바르지 않습니다."
        )
        @DecimalMin(
                value = "-90.0",
                message = "위도는 -90 이상이어야 합니다."
        )
        @DecimalMax(
                value = "90.0",
                message = "위도는 90 이하여야 합니다."
        )
        BigDecimal latitude,

        @Digits(
                integer = 3,
                fraction = 7,
                message = "경도 형식이 올바르지 않습니다."
        )
        @DecimalMin(
                value = "-180.0",
                message = "경도는 -180 이상이어야 합니다."
        )
        @DecimalMax(
                value = "180.0",
                message = "경도는 180 이하여야 합니다."
        )
        BigDecimal longitude

) {
}