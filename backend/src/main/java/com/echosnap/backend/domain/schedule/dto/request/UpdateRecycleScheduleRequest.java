package com.echosnap.backend.domain.schedule.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record UpdateRecycleScheduleRequest(

        /**
         * 변경할 배출 요일
         *
         * 상시 배출로 변경하는 경우에는 null로 전달합니다.
         */
        DayOfWeek dayOfWeek,

        /**
         * 변경할 배출 시작 시간
         *
         * 상시 배출로 변경하는 경우에는 null로 전달합니다.
         */
        LocalTime startTime,

        /**
         * 변경할 배출 종료 시간
         *
         * 상시 배출로 변경하는 경우에는 null로 전달합니다.
         */
        LocalTime endTime,

        /**
         * 상시 배출 일정으로 변경할지 여부
         */
        @NotNull(message = "상시 배출 여부는 필수입니다.")
        Boolean alwaysAvailable

) {
}