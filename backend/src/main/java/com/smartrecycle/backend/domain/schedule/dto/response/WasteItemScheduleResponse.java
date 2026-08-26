package com.smartrecycle.backend.domain.schedule.dto.response;

import com.smartrecycle.backend.domain.schedule.entity.ScheduleException;
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
     *
     * 단, 오늘 ScheduleException이 있다면
     * 예외 일정 기준으로 계산됩니다.
     */
    boolean availableToday,

    /**
     * 현재 시각을 기준으로
     * 실제 배출 가능한지 여부
     *
     * 오늘 ScheduleException이 있다면
     * 예외 일정이 우선 적용됩니다.
     */
    boolean availableNow,

    /**
     * 가장 가까운 배출 가능 날짜
     *
     * ScheduleException으로 인해
     * 정기 일정이 취소된 날짜는 건너뜁니다.
     */
    LocalDate nextAvailableDate,

    /**
     * 가장 가까운 배출 시작 일시
     *
     * 상시 배출 일정이거나
     * 등록된 일정이 없다면 null입니다.
     */
    LocalDateTime nextAvailableAt,

    /**
     * 해당 아파트에 등록된 품목의
     * 반복 공식 일정
     */
    List<RecycleScheduleTimeResponse> schedules,

    /**
     * 오늘 날짜에 적용되는 공식 예외 일정입니다.
     *
     * 예외가 없으면 null입니다.
     */
    ScheduleExceptionResponse todayException

) {

    /**
     * 기존 코드와의 호환을 위한 생성 메서드입니다.
     *
     * 다음 단계에서 ScheduleException 계산을 연결하기 전까지
     * 기존 Service도 그대로 컴파일될 수 있습니다.
     */
    public static WasteItemScheduleResponse of(
        WasteItem wasteItem,
        boolean availableToday,
        boolean availableNow,
        LocalDate nextAvailableDate,
        LocalDateTime nextAvailableAt,
        List<RecycleScheduleTimeResponse> schedules
    ) {
        return of(
            wasteItem,
            availableToday,
            availableNow,
            nextAvailableDate,
            nextAvailableAt,
            schedules,
            null
        );
    }

    /**
     * ScheduleException까지 포함하는
     * 최종 응답 생성 메서드입니다.
     */
    public static WasteItemScheduleResponse of(
        WasteItem wasteItem,
        boolean availableToday,
        boolean availableNow,
        LocalDate nextAvailableDate,
        LocalDateTime nextAvailableAt,
        List<RecycleScheduleTimeResponse> schedules,
        ScheduleException todayException
    ) {
        return new WasteItemScheduleResponse(
            WasteItemSummaryResponse.from(
                wasteItem
            ),
            availableToday,
            availableNow,
            nextAvailableDate,
            nextAvailableAt,
            List.copyOf(
                schedules
            ),
            todayException != null
                ? ScheduleExceptionResponse.from(
                todayException
            )
                : null
        );
    }
}