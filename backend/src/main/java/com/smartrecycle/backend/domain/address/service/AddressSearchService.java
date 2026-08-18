package com.smartrecycle.backend.domain.address.service;

import com.smartrecycle.backend.domain.address.client.KakaoAddressClient;
import com.smartrecycle.backend.domain.address.dto.external.KakaoAddressSearchResponse;
import com.smartrecycle.backend.domain.address.dto.response.AddressSearchResultResponse;
import com.smartrecycle.backend.global.exception.CustomException;
import com.smartrecycle.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressSearchService {

  private static final int MIN_PAGE = 1;
  private static final int MAX_PAGE = 45;
  private static final int MIN_SIZE = 1;
  private static final int MAX_SIZE = 30;

  private final KakaoAddressClient kakaoAddressClient;

  /**
   * 카카오 주소 검색 API를 호출하고
   * 외부 응답을 SmartRecycle 내부 주소 DTO로 변환합니다.
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

      if (response == null || response.documents() == null) {
        throw new CustomException(
            ErrorCode.ADDRESS_SEARCH_API_ERROR
        );
      }

      return response.documents()
          .stream()
          .map(AddressSearchResultResponse::from)
          .toList();

    } catch (RestClientException e) {
      throw new CustomException(
          ErrorCode.ADDRESS_SEARCH_API_ERROR
      );
    }
  }

  private void validateSearchCondition(
      String query,
      int page,
      int size
  ) {
    if (query == null || query.isBlank()) {
      throw new CustomException(
          ErrorCode.INVALID_ADDRESS_SEARCH_CONDITION
      );
    }

    if (page < MIN_PAGE || page > MAX_PAGE) {
      throw new CustomException(
          ErrorCode.INVALID_ADDRESS_SEARCH_CONDITION
      );
    }

    if (size < MIN_SIZE || size > MAX_SIZE) {
      throw new CustomException(
          ErrorCode.INVALID_ADDRESS_SEARCH_CONDITION
      );
    }
  }
}