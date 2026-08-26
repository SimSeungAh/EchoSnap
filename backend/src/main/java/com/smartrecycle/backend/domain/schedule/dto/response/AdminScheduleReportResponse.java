package com.smartrecycle.backend.domain.schedule.dto.response;

import com.smartrecycle.backend.domain.schedule.entity.ScheduleReport;

/**
 * 관리자용 주민 일정 제보 응답입니다.
 *
 * 주민용 응답과 달리
 * 현재 사용자의 확인 가능 여부나
 * myConfirmationValue는 필요하지 않습니다.
 *
 * 관리자는 제보 내용과 주민 확인 집계를
 * 검토 자료로 사용합니다.
 */
public record AdminScheduleReportResponse(

    ScheduleReportResponse report,

    /**
     * "맞아요"를 선택한 주민 수
     */
    long confirmedCount,

    /**
     * "정보가 달라요"를 선택한 주민 수
     */
    long differentCount,

    /**
     * 전체 주민 확인 참여 수
     */
    long totalConfirmationCount

) {

  public static AdminScheduleReportResponse of(
      ScheduleReport report,
      long confirmedCount,
      long differentCount
  ) {
    return new AdminScheduleReportResponse(
        ScheduleReportResponse.from(
            report
        ),
        confirmedCount,
        differentCount,
        confirmedCount + differentCount
    );
  }
}