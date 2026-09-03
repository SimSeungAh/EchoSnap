package com.echosnap.backend.domain.schedule.dto.request;

import jakarta.validation.constraints.Size;

/**
 * 관리자가 주민 일정 제보를
 * 승인할 때 사용하는 요청입니다.
 */
public record ApproveScheduleReportRequest(

    /**
     * 관리자 내부 검토 메모입니다.
     *
     * 없어도 승인할 수 있습니다.
     */
    @Size(
        max = 1000,
        message = "관리자 검토 메모는 1000자 이하여야 합니다."
    )
    String reviewNote,

    /**
     * TEMPORARY_CHANGE 승인 시
     * 실제 사용자 화면에 보여줄
     * 예외 일정 사유입니다.
     *
     * INITIAL_SCHEDULE /
     * SCHEDULE_CORRECTION에서는
     * 사용하지 않습니다.
     *
     * TEMPORARY_CHANGE에서는
     * Service에서 필수로 검증합니다.
     *
     * 예:
     * "추석 연휴로 인해 10월 1일은 수거하지 않습니다."
     */
    @Size(
        max = 1000,
        message = "예외 일정 안내 사유는 1000자 이하여야 합니다."
    )
    String publicReason

) {
}