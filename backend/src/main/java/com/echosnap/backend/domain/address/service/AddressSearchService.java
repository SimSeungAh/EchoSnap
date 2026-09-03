package com.echosnap.backend.domain.address.service;

import com.echosnap.backend.domain.address.client.KakaoAddressClient;
import com.echosnap.backend.domain.address.client.JusoAddressClient;
import com.echosnap.backend.domain.address.dto.external.JusoAddressSearchResponse;
import com.echosnap.backend.domain.address.dto.external.KakaoAddressSearchResponse;
import com.echosnap.backend.domain.address.dto.external.KakaoCoordinateRegionResponse;
import com.echosnap.backend.domain.address.dto.response.AddressSearchResultResponse;
import com.echosnap.backend.global.exception.CustomException;
import com.echosnap.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddressSearchService {

  private static final Logger log =
      LoggerFactory.getLogger(
          AddressSearchService.class
      );

  private static final int MIN_PAGE = 1;
  private static final int MAX_PAGE = 45;
  private static final int MIN_SIZE = 1;
  private static final int MAX_SIZE = 30;

  private static final String
      ADMINISTRATIVE_REGION_TYPE = "H";

  private final KakaoAddressClient
      kakaoAddressClient;

  private final JusoAddressClient jusoAddressClient;

  /**
   * 카카오 주소 검색 API를 호출하고
   * 외부 응답을 EchoSnap 내부 주소 DTO로 변환합니다.
   *
   * 주소 검색 API 결과에 행정동 정보가 없는 경우에는
   * 해당 주소의 좌표를 사용해서
   * 좌표 -> 행정구역정보 API를 추가 호출하고
   * 행정동 정보를 보완합니다.
   */
  public List<AddressSearchResultResponse> search(
      String query,
      int page,
      int size
  ) {
    validateSearchCondition(
        query,
        page,
        size
    );

    try {
      KakaoAddressSearchResponse response =
          kakaoAddressClient.searchAddress(
              query.trim(),
              page,
              size
          );

      if (
          response == null
              || response.documents() == null
      ) {
        throw new CustomException(
            ErrorCode.ADDRESS_SEARCH_API_ERROR
        );
      }

      List<AddressSearchResultResponse> results = response.documents()
          .stream()
          .map(
              this::toAddressSearchResult
          )
          .toList();

      return enrichWithBuildingInformation(
          query.trim(),
          size,
          results
      );

    } catch (RestClientException e) {
      throw new CustomException(
          ErrorCode.ADDRESS_SEARCH_API_ERROR
      );
    }
  }

  /**
   * 도로명주소 API가 설정된 경우 건물관리번호와 공동주택 여부를 보완합니다.
   * 보조 API 장애가 주소 검색 전체를 막지 않도록 기존 결과를 그대로 반환합니다.
   */
  private List<AddressSearchResultResponse> enrichWithBuildingInformation(
      String query,
      int size,
      List<AddressSearchResultResponse> results
  ) {
    if (!jusoAddressClient.isConfigured() || results.isEmpty()) {
      return results;
    }

    try {
      Map<String, JusoAddressSearchResponse.Juso> byRoadAddress =
          jusoAddressClient.search(query, size).stream()
              .filter(item -> hasText(firstNonBlank(item.roadAddrPart1(), item.roadAddr())))
              .collect(Collectors.toMap(
                  item -> normalizeAddress(firstNonBlank(item.roadAddrPart1(), item.roadAddr())),
                  Function.identity(),
                  (first, ignored) -> first
              ));

      return results.stream().map(result -> {
        JusoAddressSearchResponse.Juso match = byRoadAddress.get(
            normalizeAddress(result.roadAddress())
        );
        if (match == null) {
          return result;
        }
        return new AddressSearchResultResponse(
            result.addressName(), result.roadAddress(), result.jibunAddress(),
            firstNonBlank(result.buildingName(), match.bdNm()), result.zoneNo(),
            result.sido(), result.sigungu(), result.legalDong(),
            result.administrativeDong(), result.legalDongCode(),
            result.administrativeDongCode(), match.bdMgtSn(),
            match.isApartment(), result.latitude(), result.longitude()
        );
      }).toList();
    } catch (RuntimeException exception) {
      log.warn("도로명주소 건물정보 보완 실패. query={}", query, exception);
      return results;
    }
  }

  private String normalizeAddress(String value) {
    return value == null ? "" : value.replaceAll("\\s+", "").trim();
  }

  /**
   * 주소 검색 결과를 EchoSnap 응답으로 변환합니다.
   *
   * 주소 검색 결과에 행정동과 행정동 코드가
   * 모두 존재하면 추가 API를 호출하지 않습니다.
   *
   * 둘 중 하나라도 없다면 좌표를 이용해
   * 행정동 정보를 보완합니다.
   */
  private AddressSearchResultResponse
  toAddressSearchResult(
      KakaoAddressSearchResponse.Document document
  ) {
    AddressSearchResultResponse base =
        AddressSearchResultResponse.from(
            document
        );

    if (
        hasText(
            base.administrativeDong()
        )
            && hasText(
            base.administrativeDongCode()
        )
    ) {
      return base;
    }

    if (
        !hasText(
            document.x()
        )
            || !hasText(
            document.y()
        )
    ) {
      log.warn(
          "주소 검색 결과의 행정동 정보를 "
              + "보완할 수 없습니다. "
              + "addressName={}, reason=no-coordinate",
          document.addressName()
      );

      return base;
    }

    Optional<KakaoCoordinateRegionResponse.Document>
        administrativeRegion =
        findAdministrativeRegion(
            document
        );

    if (administrativeRegion.isEmpty()) {
      log.warn(
          "좌표 기반 행정동 조회 결과가 없습니다. "
              + "addressName={}, x={}, y={}",
          document.addressName(),
          document.x(),
          document.y()
      );

      return base;
    }

    KakaoCoordinateRegionResponse.Document region =
        administrativeRegion.get();

    String administrativeDong =
        firstNonBlank(
            base.administrativeDong(),
            region.region3DepthName()
        );

    String administrativeDongCode =
        firstNonBlank(
            base.administrativeDongCode(),
            region.code()
        );

    log.info(
        "주소 행정동 정보 보완 완료. "
            + "addressName={}, administrativeDong={}, "
            + "administrativeDongCode={}",
        document.addressName(),
        administrativeDong,
        administrativeDongCode
    );

    return new AddressSearchResultResponse(
        base.addressName(),
        base.roadAddress(),
        base.jibunAddress(),
        base.buildingName(),
        base.zoneNo(),
        base.sido(),
        base.sigungu(),
        base.legalDong(),
        administrativeDong,
        base.legalDongCode(),
        administrativeDongCode,
        base.buildingManagementNumber(),
        base.apartment(),
        base.latitude(),
        base.longitude()
    );
  }

  /**
   * 주소 검색 결과의 좌표를 이용해
   * 행정동(H) 정보를 찾습니다.
   *
   * 보조 조회가 실패하더라도 주소 검색 기능 자체는
   * 사용할 수 있도록 예외를 밖으로 전달하지 않고
   * Optional.empty()를 반환합니다.
   */
  private Optional<
      KakaoCoordinateRegionResponse.Document
      >
  findAdministrativeRegion(
      KakaoAddressSearchResponse.Document document
  ) {
    try {
      KakaoCoordinateRegionResponse response =
          kakaoAddressClient
              .searchRegionByCoordinate(
                  document.x(),
                  document.y()
              );

      if (
          response == null
              || response.documents() == null
      ) {
        return Optional.empty();
      }

      return response.documents()
          .stream()
          .filter(
              region ->
                  ADMINISTRATIVE_REGION_TYPE
                      .equalsIgnoreCase(
                          region.regionType()
                      )
          )
          .findFirst();

    } catch (RestClientException e) {
      log.warn(
          "좌표 기반 행정동 조회 실패. "
              + "addressName={}, x={}, y={}",
          document.addressName(),
          document.x(),
          document.y(),
          e
      );

      return Optional.empty();
    }
  }

  private void validateSearchCondition(
      String query,
      int page,
      int size
  ) {
    if (
        query == null
            || query.isBlank()
    ) {
      throw new CustomException(
          ErrorCode.INVALID_ADDRESS_SEARCH_CONDITION
      );
    }

    if (
        page < MIN_PAGE
            || page > MAX_PAGE
    ) {
      throw new CustomException(
          ErrorCode.INVALID_ADDRESS_SEARCH_CONDITION
      );
    }

    if (
        size < MIN_SIZE
            || size > MAX_SIZE
    ) {
      throw new CustomException(
          ErrorCode.INVALID_ADDRESS_SEARCH_CONDITION
      );
    }
  }

  private String firstNonBlank(
      String primary,
      String fallback
  ) {
    if (hasText(primary)) {
      return primary;
    }

    if (hasText(fallback)) {
      return fallback;
    }

    return null;
  }

  private boolean hasText(
      String value
  ) {
    return value != null
        && !value.isBlank();
  }
}
