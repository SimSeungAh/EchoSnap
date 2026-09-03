package com.echosnap.backend.domain.schedule.dto.request;

import com.echosnap.backend.domain.schedule.entity.ScheduleReportType;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 공동주택 주민이 등록하는
 * 배출 일정 제보 요청입니다.
 *
 * apartmentId는 받지 않습니다.
 *
 * 로그인 사용자의 실제 거주 Apartment를
 * 서버에서 직접 사용하여
 * 다른 공동주택에 임의로 제보하는 것을 방지합니다.
 */
public record CreateApartmentScheduleReportRequest(

    /**
     * 최초 일정 제보 /
     * 정기 일정 정정 /
     * 특정 날짜 변경
     */
    @NotNull(
        message = "제보 유형은 필수입니다."
    )
    ScheduleReportType reportType,

    /**
     * 일정이 적용되는 폐기물 품목입니다.
     */
    @NotNull(
        message = "폐기물 품목 ID는 필수입니다."
    )
    @Positive(
        message = "폐기물 품목 ID는 1 이상이어야 합니다."
    )
    Long wasteItemId,

    /**
     * 수정 또는 일시 변경할
     * 기존 공식 일정 ID입니다.
     *
     * INITIAL_SCHEDULE:
     * 일반적으로 null
     *
     * SCHEDULE_CORRECTION:
     * 필수
     *
     * TEMPORARY_CHANGE:
     * 기존 일정 변경을 제보한다면 필수
     *
     * 타입별 최종 검증은 Service에서 수행합니다.
     */
    @Positive(
        message = "기존 일정 ID는 1 이상이어야 합니다."
    )
    Long referenceScheduleId,

    /**
     * 주민이 제보하는 배출 요일입니다.
     *
     * 상시 배출 제보에서는 null입니다.
     */
    DayOfWeek reportedDayOfWeek,

    /**
     * 주민이 제보하는 배출 시작 시간입니다.
     */
    LocalTime reportedStartTime,

    /**
     * 주민이 제보하는 배출 종료 시간입니다.
     */
    LocalTime reportedEndTime,

    /**
     * 상시 배출 제보인지 여부입니다.
     *
     * 타입에 따라 Service에서
     * 입력 조합을 검증합니다.
     */
    Boolean reportedAlwaysAvailable,

    /**
     * TEMPORARY_CHANGE가 적용되는 날짜입니다.
     *
     * 예:
     * 명절 당일 수거 중단
     * 시설 점검으로 하루 일정 변경
     */
    @FutureOrPresent(
        message = "일시 변경 날짜는 오늘 또는 이후 날짜여야 합니다."
    )
    LocalDate effectiveDate,

    /**
     * 해당 날짜에 아예
     * 배출/수거가 불가능한지 여부입니다.
     *
     * TEMPORARY_CHANGE에서 사용합니다.
     */
    Boolean temporaryUnavailable,

    /**
     * 주민이 제보 이유나 근거를 설명합니다.
     *
     * 예:
     * "관리사무소 공지에서 이번 주 목요일 수거가
     * 금요일로 변경됐다고 안내했습니다."
     */
    @Size(
        max = 1000,
        message = "제보 내용은 1000자 이하여야 합니다."
    )
    String reportNote

) {
}