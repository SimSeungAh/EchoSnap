package com.echosnap.backend.domain.schedule.dto.response;

import com.echosnap.backend.domain.schedule.entity.ScheduleConfirmation;
import com.echosnap.backend.domain.schedule.entity.ScheduleConfirmationValue;

import java.time.LocalDateTime;

/**
 * 주민 제보 확인 결과 응답입니다.
 */
public record ScheduleConfirmationResponse(

    Long reportId,

    Long confirmationId,

    Long confirmerId,

    ScheduleConfirmationValue myValue,

    /**
     * "맞아요" 확인 수
     */
    long confirmedCount,

    /**
     * "정보가 달라요" 확인 수
     */
    long differentCount,

    LocalDateTime createdAt,

    LocalDateTime updatedAt

) {

  public static ScheduleConfirmationResponse of(
      ScheduleConfirmation confirmation,
      long confirmedCount,
      long differentCount
  ) {
    return new ScheduleConfirmationResponse(
        confirmation
            .getScheduleReport()
            .getId(),

        confirmation.getId(),

        confirmation
            .getConfirmer()
            .getId(),

        confirmation.getValue(),

        confirmedCount,

        differentCount,

        confirmation.getCreatedAt(),

        confirmation.getUpdatedAt()
    );
  }
}