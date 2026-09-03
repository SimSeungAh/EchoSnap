package com.echosnap.backend.domain.schedule.dto.response;

import com.echosnap.backend.domain.collectionarea.entity.CollectionArea;
import com.echosnap.backend.domain.collectionarea.entity.CollectionWasteType;
import com.echosnap.backend.domain.schedule.entity.CollectionAreaSchedule;
import com.echosnap.backend.domain.schedule.entity.ScheduleException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 일반주택 사용자에게 보여주는
 * 폐기물 종류별 지역 배출 일정입니다.
 */
public record GeneralHousingWasteScheduleResponse(

    CollectionWasteType wasteType,

    String wasteTypeLabel,

    boolean collectionAreaMatched,

    Long collectionAreaId,

    String collectionAreaName,

    String targetAreaName,

    /**
     * 반복되는 CollectionAreaSchedule이
     * 존재하는지 여부입니다.
     */
    boolean scheduleAvailable,

    String emissionDays,

    LocalTime startTime,

    LocalTime endTime,

    boolean overnight,

    boolean dayPatternParsed,

    boolean availableToday,

    boolean availableNow,

    LocalDate nextAvailableDate,

    LocalDateTime nextAvailableAt,

    String emissionMethod,

    String emissionPlace,

    String emissionPlaceType,

    String uncollectedDay,

    /**
     * 오늘 날짜에 적용되는
     * 공식 예외 일정입니다.
     */
    ScheduleExceptionResponse todayException

) {

  /**
   * CollectionArea 자체가
   * 아직 매칭되지 않은 경우
   */
  public static GeneralHousingWasteScheduleResponse unmatched(
      CollectionWasteType wasteType
  ) {
    return new GeneralHousingWasteScheduleResponse(
        wasteType,
        getWasteTypeLabel(
            wasteType
        ),
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
        null,
        null
    );
  }

  /**
   * CollectionArea는 매칭되었지만
   * 정기 일정과 현재 적용할 예외가 없는 경우
   */
  public static GeneralHousingWasteScheduleResponse
  matchedWithoutSchedule(
      CollectionWasteType wasteType,
      CollectionArea collectionArea
  ) {
    return matchedWithoutSchedule(
        wasteType,
        collectionArea,
        false,
        false,
        null,
        null,
        null
    );
  }

  /**
   * 반복 정기 일정은 없지만
   * 특정 날짜 ScheduleException이 존재할 수 있는 경우입니다.
   */
  public static GeneralHousingWasteScheduleResponse
  matchedWithoutSchedule(
      CollectionWasteType wasteType,
      CollectionArea collectionArea,
      boolean availableToday,
      boolean availableNow,
      LocalDate nextAvailableDate,
      LocalDateTime nextAvailableAt,
      ScheduleException todayException
  ) {
    return new GeneralHousingWasteScheduleResponse(
        wasteType,
        getWasteTypeLabel(
            wasteType
        ),
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
        availableToday,
        availableNow,
        nextAvailableDate,
        nextAvailableAt,
        null,
        null,
        null,
        null,
        todayException != null
            ? ScheduleExceptionResponse.from(
            todayException
        )
            : null
    );
  }

  /**
   * 기존 호출과의 호환용
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
    return of(
        wasteType,
        collectionArea,
        schedule,
        dayPatternParsed,
        availableToday,
        availableNow,
        nextAvailableDate,
        nextAvailableAt,
        null
    );
  }

  /**
   * 정기 일정 + 오늘 예외 일정
   */
  public static GeneralHousingWasteScheduleResponse of(
      CollectionWasteType wasteType,
      CollectionArea collectionArea,
      CollectionAreaSchedule schedule,
      boolean dayPatternParsed,
      boolean availableToday,
      boolean availableNow,
      LocalDate nextAvailableDate,
      LocalDateTime nextAvailableAt,
      ScheduleException todayException
  ) {
    return new GeneralHousingWasteScheduleResponse(
        wasteType,
        getWasteTypeLabel(
            wasteType
        ),
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
        schedule.getUncollectedDay(),
        todayException != null
            ? ScheduleExceptionResponse.from(
            todayException
        )
            : null
    );
  }

  private static String getWasteTypeLabel(
      CollectionWasteType wasteType
  ) {
    return switch (wasteType) {
      case LIFE_WASTE ->
          "생활쓰레기";

      case FOOD_WASTE ->
          "음식물쓰레기";

      case RECYCLABLE ->
          "재활용품";
    };
  }
}