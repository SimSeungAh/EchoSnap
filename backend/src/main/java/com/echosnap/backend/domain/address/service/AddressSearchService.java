package com.echosnap.backend.domain.address.service;

import com.echosnap.backend.domain.address.client.KakaoAddressClient;
import com.echosnap.backend.domain.address.client.JusoAddressClient;
import com.echosnap.backend.domain.address.dto.external.JusoAddressSearchResponse;
import com.echosnap.backend.domain.address.dto.external.KakaoAddressSearchResponse;
import com.echosnap.backend.domain.address.dto.external.KakaoCoordinateRegionResponse;
import com.echosnap.backend.domain.address.dto.external.KakaoKeywordSearchResponse;
import com.echosnap.backend.domain.address.dto.response.AddressSearchResultResponse;
import com.echosnap.backend.global.exception.CustomException;
import com.echosnap.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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

      results = supplementWithKeywordResults(query.trim(), page, size, results);

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
   * 주소 API가 이해하지 못하는 아파트명/건물명은 장소 키워드 API로 찾은 뒤,
   * 해당 장소의 실제 주소를 주소 API에 다시 조회해 동일한 내부 주소 형식으로 만듭니다.
   */
  private List<AddressSearchResultResponse> supplementWithKeywordResults(
      String query,
      int page,
      int size,
      List<AddressSearchResultResponse> addressResults
  ) {
    LinkedHashMap<String, AddressSearchResultResponse> merged = new LinkedHashMap<>();
    addressResults.forEach(result -> merged.put(addressKey(result), result));

    if (merged.size() >= size) {
      return List.copyOf(merged.values());
    }

    KakaoKeywordSearchResponse keywordResponse =
        kakaoAddressClient.searchKeyword(query, page, size);
    if (keywordResponse == null || keywordResponse.documents() == null) {
      return List.copyOf(merged.values());
    }

    for (KakaoKeywordSearchResponse.Document place : keywordResponse.documents()) {
      if (merged.size() >= size) {
        break;
      }

      String address = firstNonBlank(place.roadAddressName(), place.addressName());
      if (!hasText(address)) {
        continue;
      }

      try {
        KakaoAddressSearchResponse resolved =
            kakaoAddressClient.searchAddress(address, 1, 1);
        if (resolved == null || resolved.documents() == null || resolved.documents().isEmpty()) {
          continue;
        }

        AddressSearchResultResponse result =
            toAddressSearchResult(resolved.documents().get(0));
        if (!hasText(result.buildingName()) && hasText(place.placeName())) {
          result = withBuildingName(result, place.placeName());
        }
        merged.putIfAbsent(addressKey(result), result);
      } catch (RestClientException exception) {
        log.debug("키워드 장소의 주소 변환 실패. address={}", address);
      }
    }

    return new ArrayList<>(merged.values());
  }

  private String addressKey(AddressSearchResultResponse result) {
    return normalizeAddress(firstNonBlank(result.roadAddress(), result.jibunAddress()));
  }

  private AddressSearchResultResponse withBuildingName(
      AddressSearchResultResponse result,
      String buildingName
  ) {
    return new AddressSearchResultResponse(
        result.addressName(), result.roadAddress(), result.jibunAddress(), buildingName,
        result.zoneNo(), result.sido(), result.sigungu(), result.legalDong(),
        result.administrativeDong(), result.legalDongCode(),
        result.administrativeDongCode(), result.buildingManagementNumber(),
        result.apartment(), result.latitude(), result.longitude()
    );
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
        JusoAddressSearchResponse.Juso match = findBuildingInformation(
            result,
            byRoadAddress
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

  private JusoAddressSearchResponse.Juso findBuildingInformation(
      AddressSearchResultResponse result,
      Map<String, JusoAddressSearchResponse.Juso> initialMatches
  ) {
    JusoAddressSearchResponse.Juso match = initialMatches.get(
        normalizeAddress(result.roadAddress())
    );
    if (match != null || !hasText(result.roadAddress())) {
      return match;
    }

    try {
      List<JusoAddressSearchResponse.Juso> candidates =
          jusoAddressClient.search(result.roadAddress(), 10);
      String roadAddress = normalizeAddress(result.roadAddress());
      String jibunAddress = normalizeAddress(result.jibunAddress());

      Optional<JusoAddressSearchResponse.Juso> exactMatch = candidates.stream()
          .filter(item ->
              roadAddress.equals(normalizeAddress(
                  firstNonBlank(item.roadAddrPart1(), item.roadAddr())
              ))
                  || (!jibunAddress.isEmpty()
                  && jibunAddress.equals(normalizeAddress(item.jibunAddr()))))
          .findFirst();
      if (exactMatch.isPresent()) {
        return exactMatch.get();
      }

      return candidates.size() == 1 ? candidates.get(0) : null;
    } catch (RuntimeException exception) {
      log.debug("도로명주소별 건물정보 조회 실패. address={}", result.roadAddress());
      return null;
    }
  }

  private String normalizeAddress(String value) {
    if (value == null) {
      return "";
    }
    return normalizeProvinceName(value)
        .replaceAll("\\s+", "")
        .trim();
  }

  private String normalizeProvinceName(String value) {
    return value
        .replaceFirst("서울특별시", "서울")
        .replaceFirst("부산광역시", "부산")
        .replaceFirst("대구광역시", "대구")
        .replaceFirst("인천광역시", "인천")
        .replaceFirst("광주광역시", "광주")
        .replaceFirst("대전광역시", "대전")
        .replaceFirst("울산광역시", "울산")
        .replaceFirst("세종특별자치시", "세종")
        .replaceFirst("강원특별자치도", "강원")
        .replaceFirst("전북특별자치도", "전북")
        .replaceFirst("제주특별자치도", "제주");
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
