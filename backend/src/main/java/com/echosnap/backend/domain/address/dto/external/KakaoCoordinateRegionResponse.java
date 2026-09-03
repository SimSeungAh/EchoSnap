package com.echosnap.backend.domain.address.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 카카오 좌표 -> 행정구역정보 변환 API 응답 DTO입니다.
 *
 * 주소 검색 API에서 행정동 정보가 제공되지 않는 경우,
 * 주소 검색 결과의 좌표를 이용해 행정동(H) 정보를
 * 보완하기 위해 사용합니다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoCoordinateRegionResponse(
    List<Document> documents
) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Document(
      @JsonProperty("region_type")
      String regionType,

      String code,

      @JsonProperty("address_name")
      String addressName,

      @JsonProperty("region_1depth_name")
      String region1DepthName,

      @JsonProperty("region_2depth_name")
      String region2DepthName,

      @JsonProperty("region_3depth_name")
      String region3DepthName,

      @JsonProperty("region_4depth_name")
      String region4DepthName
  ) {
  }
}