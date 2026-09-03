package com.echosnap.backend.domain.waste.dto.response;

import com.echosnap.backend.domain.schedule.dto.response.RecycleScheduleTimeResponse;
import com.echosnap.backend.domain.schedule.dto.response.WasteItemScheduleResponse;
import com.echosnap.backend.domain.waste.entity.RecycleGuide;
import com.echosnap.backend.domain.waste.entity.RecycleGuideCheckItem;
import com.echosnap.backend.domain.waste.entity.WasteItem;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record WasteItemDetailResponse(

        /**
         * 폐기물 품목 ID
         */
        Long id,

        /**
         * 폐기물 품목명
         */
        String name,

        /**
         * 품목 대표 이미지 주소
         */
        String imageUrl,

        /**
         * 소속 폐기물 카테고리
         */
        WasteCategorySummaryResponse category,

        /**
         * 품목별 분리배출 가이드
         *
         * 가이드가 등록되지 않았다면 null입니다.
         */
        RecycleGuideResponse guide,

        /**
         * 오늘 해당 품목의 배출 일정이 존재하는지 여부
         *
         * 현재 배출 시간이 지났더라도
         * 오늘 일정이 있다면 true입니다.
         */
        boolean availableToday,

        /**
         * 현재 시각을 기준으로
         * 실제 배출 가능한지 여부
         */
        boolean availableNow,

        /**
         * 현재 시각을 기준으로 가장 가까운 배출 날짜
         *
         * 등록된 일정이 없다면 null입니다.
         */
        LocalDate nextAvailableDate,

        /**
         * 가장 가까운 배출 시작 일시
         *
         * 상시 배출 일정이거나 일정이 없다면 null입니다.
         */
        LocalDateTime nextAvailableAt,

        /**
         * 사용자의 거주 아파트에 등록된
         * 해당 품목의 전체 공식 일정
         */
        List<RecycleScheduleTimeResponse> schedules

) {

    /**
     * 기존 품목 상세 응답 변환 메서드입니다.
     *
     * 일정 정보를 연결하지 않은 코드도
     * 중간 단계에서 컴파일될 수 있도록 유지합니다.
     */
    public static WasteItemDetailResponse from(
            WasteItem wasteItem,
            RecycleGuide recycleGuide,
            List<RecycleGuideCheckItem> checkItems
    ) {
        return from(
                wasteItem,
                recycleGuide,
                checkItems,
                null
        );
    }

    /**
     * 폐기물 품목, 가이드, 체크리스트,
     * 사용자 아파트 일정을 상세 응답으로 변환합니다.
     */
    public static WasteItemDetailResponse from(
            WasteItem wasteItem,
            RecycleGuide recycleGuide,
            List<RecycleGuideCheckItem> checkItems,
            WasteItemScheduleResponse schedule
    ) {
        RecycleGuideResponse guideResponse =
                createGuideResponse(
                        recycleGuide,
                        checkItems
                );

        /*
         * 일정 서비스가 아직 연결되지 않았거나
         * 일정 정보가 전달되지 않은 경우 사용할 기본값입니다.
         */
        boolean availableToday = false;
        boolean availableNow = false;
        LocalDate nextAvailableDate = null;
        LocalDateTime nextAvailableAt = null;
        List<RecycleScheduleTimeResponse> schedules =
                List.of();

        if (schedule != null) {
            availableToday =
                    schedule.availableToday();

            availableNow =
                    schedule.availableNow();

            nextAvailableDate =
                    schedule.nextAvailableDate();

            nextAvailableAt =
                    schedule.nextAvailableAt();

            schedules =
                    schedule.schedules();
        }

        return new WasteItemDetailResponse(
                wasteItem.getId(),
                wasteItem.getName(),
                wasteItem.getImageUrl(),
                WasteCategorySummaryResponse.from(
                        wasteItem.getCategory()
                ),
                guideResponse,
                availableToday,
                availableNow,
                nextAvailableDate,
                nextAvailableAt,
                schedules
        );
    }

    /**
     * 분리배출 가이드가 존재하면 응답 DTO로 변환하고,
     * 존재하지 않으면 null을 반환합니다.
     */
    private static RecycleGuideResponse
    createGuideResponse(
            RecycleGuide recycleGuide,
            List<RecycleGuideCheckItem> checkItems
    ) {
        if (recycleGuide == null) {
            return null;
        }

        return RecycleGuideResponse.from(
                recycleGuide,
                checkItems
        );
    }
}