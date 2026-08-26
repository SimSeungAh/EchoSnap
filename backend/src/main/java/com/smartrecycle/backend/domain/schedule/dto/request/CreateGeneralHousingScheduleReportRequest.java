package com.smartrecycle.backend.domain.schedule.dto.request;

import com.smartrecycle.backend.domain.collectionarea.entity.CollectionWasteType;
import com.smartrecycle.backend.domain.schedule.entity.ScheduleReportType;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 일반주택 주민이 등록하는
 * 지역 배출 일정 제보 요청입니다.
 *
 * collectionAreaId는 직접 받지 않습니다.
 *
 * 사용자의 Residence에 실제로 연결된
 * CollectionWasteType별 CollectionArea를
 * 서버가 찾아서 제보 대상에 사용합니다.
 */
public record CreateGeneralHousingScheduleReportRequest(

    /**
     * 최초 일정 /
     * 정기 일정 정정 /
     * 특정 날짜 변경
     */
    @NotNull(
        message = "제보 유형은 필수입니다."
    )
    ScheduleReportType reportType,

    /**
     * 일반주택 공공데이터 일정 구분입니다.
     *
     * LIFE_WASTE
     * FOOD_WASTE
     * RECYCLABLE
     */
    @NotNull(
        message = "폐기물 종류는 필수입니다."
    )
    CollectionWasteType wasteType,

    /**
     * 정정하거나 일시 변경하려는
     * 기존 CollectionAreaSchedule ID입니다.
     */
    @Positive(
        message = "기존 일정 ID는 1 이상이어야 합니다."
    )
    Long referenceScheduleId,

    /**
     * 주민이 제보하는 배출요일 표현입니다.
     *
     * 예:
     * 월+수+금
     * 일+화+목
     * 매일
     */
    @Size(
        max = 500,
        message = "배출요일 정보는 500자 이하여야 합니다."
    )
    String reportedEmissionDays,

    /**
     * 배출 시작 시간입니다.
     */
    LocalTime reportedStartTime,

    /**
     * 배출 종료 시간입니다.
     *
     * 일반주택은
     * 20:00 ~ 02:00처럼 자정을 넘는 일정도 허용합니다.
     */
    LocalTime reportedEndTime,

    /**
     * TEMPORARY_CHANGE가 적용되는 날짜입니다.
     */
    @FutureOrPresent(
        message = "일시 변경 날짜는 오늘 또는 이후 날짜여야 합니다."
    )
    LocalDate effectiveDate,

    /**
     * 특정 날짜에 수거 자체가 중단되는지 여부입니다.
     */
    Boolean temporaryUnavailable,

    /**
     * 주민 제보 설명입니다.
     */
    @Size(
        max = 1000,
        message = "제보 내용은 1000자 이하여야 합니다."
    )
    String reportNote

) {
}