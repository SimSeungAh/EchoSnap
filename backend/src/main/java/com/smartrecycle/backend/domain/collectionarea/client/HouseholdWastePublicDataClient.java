package com.smartrecycle.backend.domain.collectionarea.client;

import com.smartrecycle.backend.domain.collectionarea.dto.external.HouseholdWastePublicDataResponse;
import com.smartrecycle.backend.global.config.HouseholdWastePublicDataProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HouseholdWastePublicDataClient {

  private static final String RETURN_TYPE_JSON = "json";

  private final RestClient restClient;
  private final HouseholdWastePublicDataProperties properties;

  public HouseholdWastePublicDataClient(
      HouseholdWastePublicDataProperties properties
  ) {
    this.properties = properties;

    this.restClient = RestClient.builder()
        .baseUrl(properties.getRequestUrl())
        .build();
  }

  /**
   * 행정안전부 생활쓰레기배출정보를
   * 페이지 단위로 조회합니다.
   *
   * 외부 API 응답 검증과 도메인 변환은
   * 이후 Service가 담당합니다.
   */
  public HouseholdWastePublicDataResponse fetchPage(
      int pageNo,
      int numOfRows
  ) {
    return restClient.get()
        .uri(uriBuilder -> uriBuilder
            .queryParam(
                "serviceKey",
                properties.getServiceKey()
            )
            .queryParam(
                "pageNo",
                pageNo
            )
            .queryParam(
                "numOfRows",
                numOfRows
            )
            .queryParam(
                "returnType",
                RETURN_TYPE_JSON
            )
            .build()
        )
        .retrieve()
        .body(
            HouseholdWastePublicDataResponse.class
        );
  }
}