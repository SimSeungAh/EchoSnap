package com.smartrecycle.backend.domain.address.dto.response;

import com.smartrecycle.backend.domain.address.dto.external.KakaoAddressSearchResponse;

import java.math.BigDecimal;

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
        toBigDecimal(document.y()),
        toBigDecimal(document.x())
    );
  }

  private static BigDecimal toBigDecimal(
      String value
  ) {
    if (value == null || value.isBlank()) {
      return null;
    }

    return new BigDecimal(value);
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