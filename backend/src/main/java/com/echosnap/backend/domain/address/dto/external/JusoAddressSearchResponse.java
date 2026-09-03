package com.echosnap.backend.domain.address.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JusoAddressSearchResponse(Results results) {
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Results(Common common, List<Juso> juso) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Common(String errorCode, String errorMessage) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Juso(
      String roadAddr,
      String roadAddrPart1,
      String jibunAddr,
      String bdNm,
      String bdMgtSn,
      String bdKdcd
  ) {
    public boolean isApartment() {
      return "1".equals(bdKdcd);
    }
  }
}
