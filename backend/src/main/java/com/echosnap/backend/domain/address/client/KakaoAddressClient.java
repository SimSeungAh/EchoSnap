package com.echosnap.backend.domain.address.client;

import com.echosnap.backend.domain.address.dto.external.KakaoAddressSearchResponse;
import com.echosnap.backend.global.config.KakaoLocalProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class KakaoAddressClient {

  private static final String ADDRESS_SEARCH_PATH =
      "/v2/local/search/address.json";

  private final RestClient restClient;

  public KakaoAddressClient(
      KakaoLocalProperties properties
  ) {
    this.restClient = RestClient.builder()
        .baseUrl(properties.getBaseUrl())
        .defaultHeader(
            HttpHeaders.AUTHORIZATION,
            "KakaoAK " + properties.getRestApiKey()
        )
        .build();
  }

  public KakaoAddressSearchResponse searchAddress(
      String query,
      int page,
      int size
  ) {
    return restClient.get()
        .uri(uriBuilder -> uriBuilder
            .path(ADDRESS_SEARCH_PATH)
            .queryParam("query", query)
            .queryParam("page", page)
            .queryParam("size", size)
            .build()
        )
        .retrieve()
        .body(KakaoAddressSearchResponse.class);
  }
}