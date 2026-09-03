package com.echosnap.backend.domain.schedule.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 관리자가 주민 일정 제보를
 * 거절할 때 사용하는 요청입니다.
 */
public record RejectScheduleReportRequest(

    @NotBlank(
        message = "거절 사유는 필수입니다."
    )
    @Size(
        max = 1000,
        message = "거절 사유는 1000자 이하여야 합니다."
    )
    String reviewNote

) {
}