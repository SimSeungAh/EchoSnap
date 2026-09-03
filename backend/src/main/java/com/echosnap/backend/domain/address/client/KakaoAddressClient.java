package com.echosnap.backend.domain.address.client;

import com.echosnap.backend.domain.address.dto.external.KakaoAddressSearchResponse;
import com.echosnap.backend.domain.address.dto.external.KakaoCoordinateRegionResponse;
import com.echosnap.backend.domain.address.dto.external.KakaoKeywordSearchResponse;
import com.echosnap.backend.global.config.KakaoLocalProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class KakaoAddressClient {

  private static final String ADDRESS_SEARCH_PATH =
      "/v2/local/search/address.json";

  private static final String COORD_TO_REGION_PATH =
      "/v2/local/geo/coord2regioncode.json";

  private static final String KEYWORD_SEARCH_PATH =
      "/v2/local/search/keyword.json";

  private final RestClient restClient;

  public KakaoAddressClient(
      KakaoLocalProperties properties
  ) {
    this.restClient = RestClient.builder()
        .baseUrl(
            properties.getBaseUrl()
        )
        .defaultHeader(
            HttpHeaders.AUTHORIZATION,
            "KakaoAK "
                + properties.getRestApiKey()
        )
        .build();
  }

  /**
   * 문자열 주소를 검색합니다.
   */
  public KakaoAddressSearchResponse searchAddress(
      String query,
      int page,
      int size
  ) {
    return restClient.get()
        .uri(
            uriBuilder ->
                uriBuilder
                    .path(
                        ADDRESS_SEARCH_PATH
                    )
                    .queryParam(
                        "query",
                        query
                    )
                    .queryParam(
                        "page",
                        page
                    )
                    .queryParam(
                        "size",
                        size
                    )
                    .build()
        )
        .retrieve()
        .body(
            KakaoAddressSearchResponse.class
        );
  }

  /** 아파트명, 건물명처럼 주소 형식이 아닌 검색어로 장소를 찾습니다. */
  public KakaoKeywordSearchResponse searchKeyword(
      String query,
      int page,
      int size
  ) {
    return restClient.get()
        .uri(uriBuilder -> uriBuilder
            .path(KEYWORD_SEARCH_PATH)
            .queryParam("query", query)
            .queryParam("page", page)
            .queryParam("size", size)
            .build())
        .retrieve()
        .body(KakaoKeywordSearchResponse.class);
  }

  /**
   * 주소 검색 결과의 좌표를 이용해
   * 행정동/법정동 정보를 조회합니다.
   *
   * 카카오 좌표 -> 행정구역 변환 API에서는
   *
   * region_type = H
   *   행정동
   *
   * region_type = B
   *   법정동
   *
   * 으로 반환됩니다.
   */
  public KakaoCoordinateRegionResponse
  searchRegionByCoordinate(
      String x,
      String y
  ) {
    return restClient.get()
        .uri(
            uriBuilder ->
                uriBuilder
                    .path(
                        COORD_TO_REGION_PATH
                    )
                    .queryParam(
                        "x",
                        x
                    )
                    .queryParam(
                        "y",
                        y
                    )
                    .build()
        )
        .retrieve()
        .body(
            KakaoCoordinateRegionResponse.class
        );
  }
}
