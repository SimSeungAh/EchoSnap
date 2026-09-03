package com.echosnap.backend.domain.schedule.dto.response;

import com.echosnap.backend.domain.schedule.entity.ScheduleException;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 사용자에게 보여주는 특정 날짜의 공식 예외 일정입니다.
 *
 * 해당 날짜에 ScheduleException이 존재하면
 * 반복 정기 일정보다 우선 적용됩니다.
 */
public record ScheduleExceptionResponse(

    Long id,

    /**
     * 예외가 적용되는 날짜
     */
    LocalDate effectiveDate,

    /**
     * 해당 날짜에 배출/수거 자체가
     * 불가능한지 여부
     */
    boolean unavailable,

    /**
     * 변경된 시작 시간
     */
    LocalTime startTime,

    /**
     * 변경된 종료 시간
     */
    LocalTime endTime,

    /**
     * 공동주택에서 해당 날짜만
     * 상시 배출 가능한 경우 사용
     */
    Boolean alwaysAvailable,

    /**
     * 자정을 넘기는 예외 일정 여부
     *
     * 예:
     * 20:00 ~ 02:00
     */
    boolean overnight,

    /**
     * 예외 일정 안내 사유
     *
     * 예:
     * "추석 연휴로 인해 당일 수거하지 않습니다."
     */
    String reason

) {

  public static ScheduleExceptionResponse from(
      ScheduleException exception
  ) {
    return new ScheduleExceptionResponse(
        exception.getId(),
        exception.getEffectiveDate(),
        exception.isUnavailable(),
        exception.getStartTime(),
        exception.getEndTime(),
        exception.getAlwaysAvailable(),
        exception.isOvernight(),
        exception.getReason()
    );
  }
}