package com.echosnap.backend.domain.schedule.dto.response;

import com.echosnap.backend.domain.residence.entity.Residence;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 일반주택 사용자의 전체 지역 배출 일정 응답입니다.
 */
public record GeneralHousingScheduleResponse(

    Long residenceId,

    String addressName,

    String sido,

    String sigungu,

    String administrativeDong,

    String legalDong,

    LocalDateTime referenceDateTime,

    List<GeneralHousingWasteScheduleResponse> schedules

) {

  public static GeneralHousingScheduleResponse of(
      Residence residence,
      LocalDateTime referenceDateTime,
      List<GeneralHousingWasteScheduleResponse> schedules
  ) {
    return new GeneralHousingScheduleResponse(
        residence.getId(),
        residence.getAddressName(),
        residence.getSido(),
        residence.getSigungu(),
        residence.getAdministrativeDong(),
        residence.getLegalDong(),
        referenceDateTime,
        List.copyOf(schedules)
    );
  }
}