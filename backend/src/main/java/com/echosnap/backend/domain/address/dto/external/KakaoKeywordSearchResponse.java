package com.echosnap.backend.domain.address.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoKeywordSearchResponse(List<Document> documents) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Document(
      @JsonProperty("place_name") String placeName,
      @JsonProperty("address_name") String addressName,
      @JsonProperty("road_address_name") String roadAddressName,
      String x,
      String y
  ) {
  }
}
