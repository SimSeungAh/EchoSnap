package com.echosnap.backend.domain.user.dto.request;

import com.echosnap.backend.domain.residence.entity.GeneralHousingType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 일반주택 사용자가 주소 검색 결과에서 선택한 주소를
 * 자신의 현재 거주지로 설정할 때 사용하는 요청 DTO입니다.
 *
 * 주소뿐 아니라
 * 단독 / 다가구 / 연립 / 다세대 중
 * 사용자가 선택한 세부 주거형태도 함께 저장합니다.
 */
public record UpdateUserResidenceRequest(

    @NotNull(
        message = "일반주택 유형은 필수입니다."
    )
    GeneralHousingType generalHousingType,

    @NotBlank(
        message = "대표 주소는 필수입니다."
    )
    @Size(
        max = 255,
        message = "대표 주소는 255자 이하여야 합니다."
    )
    String addressName,

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

    @Size(
        max = 200,
        message = "건물명은 200자 이하여야 합니다."
    )
    String buildingName,

    @Size(
        max = 10,
        message = "우편번호는 10자 이하여야 합니다."
    )
    String zoneNo,

    @NotBlank(
        message = "시도 정보는 필수입니다."
    )
    @Size(
        max = 50,
        message = "시도 정보는 50자 이하여야 합니다."
    )
    String sido,

    @NotBlank(
        message = "시군구 정보는 필수입니다."
    )
    @Size(
        max = 100,
        message = "시군구 정보는 100자 이하여야 합니다."
    )
    String sigungu,

    @Size(
        max = 100,
        message = "법정동 정보는 100자 이하여야 합니다."
    )
    String legalDong,

    @Size(
        max = 100,
        message = "행정동 정보는 100자 이하여야 합니다."
    )
    String administrativeDong,

    @Size(
        max = 20,
        message = "법정동 코드는 20자 이하여야 합니다."
    )
    String legalDongCode,

    @Size(
        max = 20,
        message = "행정동 코드는 20자 이하여야 합니다."
    )
    String administrativeDongCode,

    @NotNull(
        message = "위도는 필수입니다."
    )
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

    @NotNull(
        message = "경도는 필수입니다."
    )
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