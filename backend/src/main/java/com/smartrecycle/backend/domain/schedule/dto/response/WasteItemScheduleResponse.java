package com.smartrecycle.backend.domain.schedule.dto.response;

import com.smartrecycle.backend.domain.waste.dto.response.WasteItemSummaryResponse;
import com.smartrecycle.backend.domain.waste.entity.WasteItem;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record WasteItemScheduleResponse(

        /**
         * 폐기물 품목 기본 정보
         */
        WasteItemSummaryResponse wasteItem,

        /**
         * 오늘 해당 품목을 배출할 수 있는 일정이 있는지 여부
         *
         * 현재 시간이 배출 시간을 지났더라도
         * 오늘 일정 자체가 존재하면 true입니다.
         */
        boolean availableToday,

        /**
         * 현재 시각을 기준으로 실제 배출 가능한지 여부
         */
        boolean availableNow,

        /**
         * 가장 가까운 배출 가능 날짜
         *
         * 등록된 일정이 없다면 null입니다.
         */
        LocalDate nextAvailableDate,

        /**
         * 가장 가까운 배출 시작 일시
         *
         * 상시 배출 일정이거나 등록된 일정이 없다면 null입니다.
         */
        LocalDateTime nextAvailableAt,

        /**
         * 해당 아파트에 등록된 품목의 전체 공식 일정
         */
        List<RecycleScheduleTimeResponse> schedules

) {

    public static WasteItemScheduleResponse of(
            WasteItem wasteItem,
            boolean availableToday,
            boolean availableNow,
            LocalDate nextAvailableDate,
            LocalDateTime nextAvailableAt,
            List<RecycleScheduleTimeResponse> schedules
    ) {
        return new WasteItemScheduleResponse(
                WasteItemSummaryResponse.from(
                        wasteItem
                ),
                availableToday,
                availableNow,
                nextAvailableDate,
                nextAvailableAt,
                List.copyOf(schedules)
        );
    }
}