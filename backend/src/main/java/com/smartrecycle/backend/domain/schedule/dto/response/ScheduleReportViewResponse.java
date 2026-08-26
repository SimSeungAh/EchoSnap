package com.smartrecycle.backend.domain.schedule.dto.response;

import com.smartrecycle.backend.domain.schedule.entity.ScheduleConfirmationValue;
import com.smartrecycle.backend.domain.schedule.entity.ScheduleReport;

/**
 * 주민 제보 목록/상세 화면에서 사용하는 응답입니다.
 *
 * 제보 원본 정보와 함께
 * 주민 확인 집계와 현재 사용자의 확인 상태를 반환합니다.
 */
public record ScheduleReportViewResponse(

    ScheduleReportResponse report,

    /**
     * "맞아요" 수
     */
    long confirmedCount,

    /**
     * "정보가 달라요" 수
     */
    long differentCount,

    /**
     * 현재 로그인 사용자가 남긴 확인 값입니다.
     *
     * 아직 확인하지 않았다면 null입니다.
     */
    ScheduleConfirmationValue myConfirmationValue,

    /**
     * 현재 로그인 사용자가
     * 이 제보에 확인을 남길 수 있는지 여부입니다.
     */
    boolean canConfirm

) {

  public static ScheduleReportViewResponse of(
      ScheduleReport report,
      long confirmedCount,
      long differentCount,
      ScheduleConfirmationValue myConfirmationValue,
      boolean canConfirm
  ) {
    return new ScheduleReportViewResponse(
        ScheduleReportResponse.from(
            report
        ),
        confirmedCount,
        differentCount,
        myConfirmationValue,
        canConfirm
    );
  }
}