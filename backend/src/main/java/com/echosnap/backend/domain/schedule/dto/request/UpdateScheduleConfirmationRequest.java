package com.echosnap.backend.domain.schedule.dto.request;

import com.echosnap.backend.domain.schedule.entity.ScheduleConfirmationValue;
import jakarta.validation.constraints.NotNull;

/**
 * 주민 일정 제보에 대한 확인 요청입니다.
 *
 * CONFIRMED:
 * 제보 내용이 실제 일정과 맞음
 *
 * DIFFERENT:
 * 제보 내용이 실제 일정과 다름
 */
public record UpdateScheduleConfirmationRequest(

    @NotNull(
        message = "확인 값은 필수입니다."
    )
    ScheduleConfirmationValue value

) {
}