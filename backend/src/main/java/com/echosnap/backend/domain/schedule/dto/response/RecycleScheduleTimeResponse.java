package com.echosnap.backend.domain.schedule.dto.response;

import com.echosnap.backend.domain.schedule.entity.RecycleSchedule;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record RecycleScheduleTimeResponse(

        /**
         * 배출 일정 ID
         */
        Long scheduleId,

        /**
         * 배출 가능한 요일
         *
         * 상시 배출 일정이면 null입니다.
         */
        DayOfWeek dayOfWeek,

        /**
         * 배출 시작 시간
         *
         * 상시 배출 일정이면 null입니다.
         */
        LocalTime startTime,

        /**
         * 배출 종료 시간
         *
         * 상시 배출 일정이면 null입니다.
         */
        LocalTime endTime,

        /**
         * 상시 배출 가능 여부
         */
        boolean alwaysAvailable

) {

    public static RecycleScheduleTimeResponse from(
            RecycleSchedule schedule
    ) {
        return new RecycleScheduleTimeResponse(
                schedule.getId(),
                schedule.getDayOfWeek(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                schedule.isAlwaysAvailable()
        );
    }
}
