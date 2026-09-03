package com.echosnap.backend.domain.address.client;

import com.echosnap.backend.domain.address.dto.external.JusoAddressSearchResponse;
import com.echosnap.backend.global.config.JusoProperties;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class JusoAddressClient {
  private final JusoProperties properties;
  private final RestClient restClient;

  public JusoAddressClient(JusoProperties properties) {
    this.properties = properties;
    this.restClient = RestClient.builder()
        .baseUrl(properties.getBaseUrl())
        .build();
  }

  public boolean isConfigured() {
    return properties.isConfigured();
  }

  public List<JusoAddressSearchResponse.Juso> search(
      String keyword,
      int size
  ) {
    JusoAddressSearchResponse response = restClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("/addrlink/addrLinkApi.do")
            .queryParam("confmKey", properties.getConfirmationKey())
            .queryParam("currentPage", 1)
            .queryParam("countPerPage", size)
            .queryParam("keyword", keyword)
            .queryParam("resultType", "json")
            .queryParam("hstryYn", "N")
            .build())
        .retrieve()
        .body(JusoAddressSearchResponse.class);

    if (response == null || response.results() == null
        || response.results().juso() == null) {
      return List.of();
    }
    return response.results().juso();
  }
}
