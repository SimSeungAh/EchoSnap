package com.echosnap.backend.domain.schedule.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record CreateRecycleScheduleRequest(

        /**
         * 공식 배출 일정을 등록할 아파트 ID
         */
        @NotNull(message = "아파트 ID는 필수입니다.")
        @Positive(message = "아파트 ID는 1 이상이어야 합니다.")
        Long apartmentId,

        /**
         * 일정이 적용될 폐기물 품목 ID
         */
        @NotNull(message = "폐기물 품목 ID는 필수입니다.")
        @Positive(message = "폐기물 품목 ID는 1 이상이어야 합니다.")
        Long wasteItemId,

        /**
         * 배출 가능한 요일
         *
         * 상시 배출인 경우에는 null로 전달합니다.
         */
        DayOfWeek dayOfWeek,

        /**
         * 배출 시작 시간
         *
         * 상시 배출인 경우에는 null로 전달합니다.
         */
        LocalTime startTime,

        /**
         * 배출 종료 시간
         *
         * 상시 배출인 경우에는 null로 전달합니다.
         */
        LocalTime endTime,

        /**
         * 요일과 시간에 관계없이 항상 배출 가능한지 여부
         *
         * true:
         * dayOfWeek, startTime, endTime은 null이어야 합니다.
         *
         * false:
         * dayOfWeek, startTime, endTime이 모두 필요합니다.
         */
        @NotNull(message = "상시 배출 여부는 필수입니다.")
        Boolean alwaysAvailable

) {
}