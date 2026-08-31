package com.smartrecycle.backend.domain.address.dto.response;

import com.smartrecycle.backend.domain.address.dto.external.KakaoAddressSearchResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Flutter 사용자 앱에 반환하는 SmartRecycle 내부 주소 검색 결과 DTO입니다.
 *
 * 외부 API의 필드명을 그대로 노출하지 않고,
 * 일반주택 주소 저장과 이후 수거구역 연결에 필요한 값만 제공합니다.
 */
public record AddressSearchResultResponse(
    String addressName,
    String roadAddress,
    String jibunAddress,
    String buildingName,
    String zoneNo,
    String sido,
    String sigungu,
    String legalDong,
    String administrativeDong,
    String legalDongCode,
    String administrativeDongCode,
    BigDecimal latitude,
    BigDecimal longitude
) {

  private static final int COORDINATE_SCALE = 7;

  public static AddressSearchResultResponse from(
      KakaoAddressSearchResponse.Document document
  ) {
    KakaoAddressSearchResponse.Address address =
        document.address();

    KakaoAddressSearchResponse.RoadAddress roadAddress =
        document.roadAddress();

    return new AddressSearchResultResponse(
        document.addressName(),
        roadAddress != null
            ? roadAddress.addressName()
            : null,
        address != null
            ? address.addressName()
            : null,
        roadAddress != null
            ? blankToNull(roadAddress.buildingName())
            : null,
        roadAddress != null
            ? blankToNull(roadAddress.zoneNo())
            : null,
        address != null
            ? address.region1DepthName()
            : roadAddress != null
              ? roadAddress.region1DepthName()
              : null,
        address != null
            ? address.region2DepthName()
            : roadAddress != null
              ? roadAddress.region2DepthName()
              : null,
        address != null
            ? address.region3DepthName()
            : roadAddress != null
              ? roadAddress.region3DepthName()
              : null,
        address != null
            ? blankToNull(address.region3DepthHName())
            : null,
        address != null
            ? blankToNull(address.bCode())
            : null,
        address != null
            ? blankToNull(address.hCode())
            : null,
        toCoordinate(document.y()),
        toCoordinate(document.x())
    );
  }

  /**
   * 카카오 API 좌표는 소수점 자릿수가 길 수 있습니다.
   *
   * SmartRecycle의 요청 DTO와 DB 좌표 컬럼은
   * 소수점 7자리 기준이므로 외부 API 응답을
   * 내부 형식으로 변환하는 시점에 동일한 정밀도로 정규화합니다.
   */
  private static BigDecimal toCoordinate(
      String value
  ) {
    if (value == null || value.isBlank()) {
      return null;
    }

    return new BigDecimal(value)
        .setScale(
            COORDINATE_SCALE,
            RoundingMode.HALF_UP
        );
  }

  private static String blankToNull(
      String value
  ) {
    if (value == null || value.isBlank()) {
      return null;
    }

    return value;
  }
}