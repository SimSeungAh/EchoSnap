package com.smartrecycle.backend.domain.address.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 카카오 로컬 주소 검색 API의 원본 응답을 매핑하는 외부 연동 DTO입니다.
 *
 * 외부 API의 필드 구조를 그대로 받아들이고,
 * SmartRecycle 내부 응답 형식으로 변환하는 책임은 별도의 내부 DTO가 담당합니다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoAddressSearchResponse(
    Meta meta,
    List<Document> documents
) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Meta(
      @JsonProperty("total_count")
      Integer totalCount,

      @JsonProperty("pageable_count")
      Integer pageableCount,

      @JsonProperty("is_end")
      Boolean isEnd
  ) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Document(
      @JsonProperty("address_name")
      String addressName,

      @JsonProperty("address_type")
      String addressType,

      String x,
      String y,
      Address address,

      @JsonProperty("road_address")
      RoadAddress roadAddress
  ) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Address(
      @JsonProperty("address_name")
      String addressName,

      @JsonProperty("region_1depth_name")
      String region1DepthName,

      @JsonProperty("region_2depth_name")
      String region2DepthName,

      @JsonProperty("region_3depth_name")
      String region3DepthName,

      @JsonProperty("region_3depth_h_name")
      String region3DepthHName,

      @JsonProperty("h_code")
      String hCode,

      @JsonProperty("b_code")
      String bCode
  ) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record RoadAddress(
      @JsonProperty("address_name")
      String addressName,

      @JsonProperty("region_1depth_name")
      String region1DepthName,

      @JsonProperty("region_2depth_name")
      String region2DepthName,

      @JsonProperty("region_3depth_name")
      String region3DepthName,

      @JsonProperty("road_name")
      String roadName,

      @JsonProperty("building_name")
      String buildingName,

      @JsonProperty("zone_no")
      String zoneNo
  ) {
  }
}