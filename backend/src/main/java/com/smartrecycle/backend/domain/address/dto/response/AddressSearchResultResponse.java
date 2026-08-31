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
        normalizeSido(
            address != null
                ? address.region1DepthName()
                : roadAddress != null
                  ? roadAddress.region1DepthName()
                  : null
        ),
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

  /**
   * 카카오 주소 API의 시도명은
   * "부산", "서울"처럼 축약형으로 내려올 수 있습니다.
   *
   * SmartRecycle의 공공데이터 수거구역은
   * "부산광역시", "서울특별시"처럼 정식 명칭을 사용하므로
   * 주소 검색 결과를 내부 DTO로 바꾸는 경계에서
   * 시도명을 하나의 표준값으로 정규화합니다.
   */
  private static String normalizeSido(
      String value
  ) {
    String sido =
        blankToNull(
            value
        );

    if (sido == null) {
      return null;
    }

    return switch (sido) {
      case "서울", "서울시", "서울특별시" ->
          "서울특별시";
      case "부산", "부산시", "부산광역시" ->
          "부산광역시";
      case "대구", "대구시", "대구광역시" ->
          "대구광역시";
      case "인천", "인천시", "인천광역시" ->
          "인천광역시";
      case "광주", "광주시", "광주광역시" ->
          "광주광역시";
      case "대전", "대전시", "대전광역시" ->
          "대전광역시";
      case "울산", "울산시", "울산광역시" ->
          "울산광역시";
      case "세종", "세종시", "세종특별자치시" ->
          "세종특별자치시";
      case "경기", "경기도" ->
          "경기도";
      case "강원", "강원도", "강원특별자치도" ->
          "강원특별자치도";
      case "충북", "충청북도" ->
          "충청북도";
      case "충남", "충청남도" ->
          "충청남도";
      case "전북", "전라북도", "전북특별자치도" ->
          "전북특별자치도";
      case "전남", "전라남도" ->
          "전라남도";
      case "경북", "경상북도" ->
          "경상북도";
      case "경남", "경상남도" ->
          "경상남도";
      case "제주", "제주도", "제주특별자치도" ->
          "제주특별자치도";
      default ->
          sido;
    };
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