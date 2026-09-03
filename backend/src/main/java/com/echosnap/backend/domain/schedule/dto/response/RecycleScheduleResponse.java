package com.echosnap.backend.domain.schedule.dto.response;

import com.echosnap.backend.domain.schedule.entity.RecycleSchedule;
import com.echosnap.backend.domain.waste.dto.response.WasteItemSummaryResponse;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record RecycleScheduleResponse(

        /**
         * 배출 일정 ID
         */
        Long id,

        /**
         * 일정이 적용되는 아파트 ID
         */
        Long apartmentId,

        /**
         * 일정이 적용되는 아파트 이름
         */
        String apartmentName,

        /**
         * 일정이 적용되는 폐기물 품목 정보
         */
        WasteItemSummaryResponse wasteItem,

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
        boolean alwaysAvailable,

        /**
         * 일정 생성 일시
         */
        LocalDateTime createdAt,

        /**
         * 일정 수정 일시
         */
        LocalDateTime updatedAt

) {

    /**
     * RecycleSchedule 엔티티를 응답 DTO로 변환합니다.
     */
    public static RecycleScheduleResponse from(
            RecycleSchedule schedule
    ) {
        return new RecycleScheduleResponse(
                schedule.getId(),
                schedule.getApartment().getId(),
                schedule.getApartment().getName(),
                WasteItemSummaryResponse.from(
                        schedule.getWasteItem()
                ),
                schedule.getDayOfWeek(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                schedule.isAlwaysAvailable(),
                schedule.getCreatedAt(),
                schedule.getUpdatedAt()
        );
    }
}