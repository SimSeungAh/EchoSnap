package com.smartrecycle.backend.domain.schedule.dto.response;

import com.smartrecycle.backend.domain.collectionarea.entity.CollectionArea;
import com.smartrecycle.backend.domain.collectionarea.entity.CollectionWasteType;
import com.smartrecycle.backend.domain.schedule.entity.CollectionAreaSchedule;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 일반주택 사용자에게 보여주는
 * 폐기물 종류별 지역 배출 일정입니다.
 *
 * LIFE_WASTE / FOOD_WASTE / RECYCLABLE
 * 세 종류를 각각 반환합니다.
 */
public record GeneralHousingWasteScheduleResponse(

    CollectionWasteType wasteType,

    String wasteTypeLabel,

    /**
     * 사용자의 주소에 해당 폐기물 종류의
     * CollectionArea가 매칭되었는지 여부
     */
    boolean collectionAreaMatched,

    Long collectionAreaId,

    String collectionAreaName,

    String targetAreaName,

    /**
     * 매칭된 CollectionArea에
     * 실제 배출 일정이 존재하는지 여부
     */
    boolean scheduleAvailable,

    /**
     * 공공데이터 원본 요일 표현
     *
     * 예:
     * 일+화+목
     * 월~금
     */
    String emissionDays,

    LocalTime startTime,

    LocalTime endTime,

    /**
     * 20:00 ~ 02:00처럼
     * 다음 날까지 이어지는 일정인지 여부
     */
    boolean overnight,

    /**
     * emissionDays 문자열을
     * 시스템이 실제 DayOfWeek로 해석할 수 있었는지 여부
     */
    boolean dayPatternParsed,

    /**
     * 오늘 시작하는 배출 일정이 있는지 여부
     */
    boolean availableToday,

    /**
     * 현재 시각에 실제 배출 가능한지 여부
     */
    boolean availableNow,

    /**
     * 가장 가까운 다음 배출 날짜
     */
    LocalDate nextAvailableDate,

    /**
     * 시작 시간이 명확한 경우
     * 가장 가까운 다음 배출 가능 시각
     */
    LocalDateTime nextAvailableAt,

    String emissionMethod,

    String emissionPlace,

    String emissionPlaceType,

    String uncollectedDay

) {

  /**
   * 주소와 수거구역이 아직 매칭되지 않은 경우입니다.
   */
  public static GeneralHousingWasteScheduleResponse unmatched(
      CollectionWasteType wasteType
  ) {
    return new GeneralHousingWasteScheduleResponse(
        wasteType,
        getWasteTypeLabel(wasteType),
        false,
        null,
        null,
        null,
        false,
        null,
        null,
        null,
        false,
        false,
        false,
        false,
        null,
        null,
        null,
        null,
        null,
        null
    );
  }

  /**
   * 수거구역은 매칭되었지만
   * 아직 일정 데이터가 없는 경우입니다.
   */
  public static GeneralHousingWasteScheduleResponse
  matchedWithoutSchedule(
      CollectionWasteType wasteType,
      CollectionArea collectionArea
  ) {
    return new GeneralHousingWasteScheduleResponse(
        wasteType,
        getWasteTypeLabel(wasteType),
        true,
        collectionArea.getId(),
        collectionArea.getAreaName(),
        collectionArea.getTargetAreaName(),
        false,
        null,
        null,
        null,
        false,
        false,
        false,
        false,
        null,
        null,
        null,
        null,
        null,
        null
    );
  }

  /**
   * 수거구역과 일정이 모두 존재하는
   * 정상 응답을 생성합니다.
   */
  public static GeneralHousingWasteScheduleResponse of(
      CollectionWasteType wasteType,
      CollectionArea collectionArea,
      CollectionAreaSchedule schedule,
      boolean dayPatternParsed,
      boolean availableToday,
      boolean availableNow,
      LocalDate nextAvailableDate,
      LocalDateTime nextAvailableAt
  ) {
    return new GeneralHousingWasteScheduleResponse(
        wasteType,
        getWasteTypeLabel(wasteType),
        true,
        collectionArea.getId(),
        collectionArea.getAreaName(),
        collectionArea.getTargetAreaName(),
        true,
        schedule.getEmissionDays(),
        schedule.getStartTime(),
        schedule.getEndTime(),
        schedule.isOvernight(),
        dayPatternParsed,
        availableToday,
        availableNow,
        nextAvailableDate,
        nextAvailableAt,
        schedule.getEmissionMethod(),
        schedule.getEmissionPlace(),
        schedule.getEmissionPlaceType(),
        schedule.getUncollectedDay()
    );
  }

  private static String getWasteTypeLabel(
      CollectionWasteType wasteType
  ) {
    return switch (wasteType) {
      case LIFE_WASTE -> "생활쓰레기";
      case FOOD_WASTE -> "음식물쓰레기";
      case RECYCLABLE -> "재활용품";
    };
  }
}