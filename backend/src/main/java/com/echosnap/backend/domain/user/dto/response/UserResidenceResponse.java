package com.echosnap.backend.domain.user.dto.response;

import com.echosnap.backend.domain.residence.entity.Residence;

import java.math.BigDecimal;

/**
 * 현재 로그인한 사용자가 설정한
 * 일반주택 주소 기반 거주지 정보입니다.
 */
public record UserResidenceResponse(

    Long id,

    String addressName,

    String roadAddress,

    String jibunAddress,

    String buildingName,

    String zoneNo,

    String sido,

    String sigungu,

    String legalDong,

    String administrativeDong,

    String legalDongCode,

    String administrativeDongCode,

    BigDecimal latitude,

    BigDecimal longitude

) {

  public static UserResidenceResponse from(
      Residence residence
  ) {
    if (residence == null) {
      return null;
    }

    return new UserResidenceResponse(
        residence.getId(),
        residence.getAddressName(),
        residence.getRoadAddress(),
        residence.getJibunAddress(),
        residence.getBuildingName(),
        residence.getZoneNo(),
        residence.getSido(),
        residence.getSigungu(),
        residence.getLegalDong(),
        residence.getAdministrativeDong(),
        residence.getLegalDongCode(),
        residence.getAdministrativeDongCode(),
        residence.getLatitude(),
        residence.getLongitude()
    );
  }
}