package com.smartrecycle.backend.domain.schedule.service;

import com.smartrecycle.backend.domain.collectionarea.entity.CollectionArea;
import com.smartrecycle.backend.domain.collectionarea.entity.CollectionWasteType;
import com.smartrecycle.backend.domain.residence.entity.Residence;
import com.smartrecycle.backend.domain.residence.entity.ResidenceCollectionArea;
import com.smartrecycle.backend.domain.schedule.dto.response.GeneralHousingScheduleResponse;
import com.smartrecycle.backend.domain.schedule.dto.response.GeneralHousingWasteScheduleResponse;
import com.smartrecycle.backend.domain.schedule.entity.CollectionAreaSchedule;
import com.smartrecycle.backend.domain.schedule.repository.CollectionAreaScheduleRepository;
import com.smartrecycle.backend.domain.user.entity.ResidenceType;
import com.smartrecycle.backend.domain.user.entity.User;
import com.smartrecycle.backend.domain.user.repository.UserRepository;
import com.smartrecycle.backend.global.exception.CustomException;
import com.smartrecycle.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CollectionAreaScheduleQueryService {

  /**
   * 월요일부터 일요일까지의 순서를
   * 명시적으로 관리합니다.
   *
   * 월~금 같은 범위 표현을 해석할 때 사용합니다.
   */
  private static final List<DayOfWeek> WEEK_ORDER =
      List.of(
          DayOfWeek.MONDAY,
          DayOfWeek.TUESDAY,
          DayOfWeek.WEDNESDAY,
          DayOfWeek.THURSDAY,
          DayOfWeek.FRIDAY,
          DayOfWeek.SATURDAY,
          DayOfWeek.SUNDAY
      );

  /**
   * 월~금
   * 월-금
   * 토∼일
   * 같은 요일 범위를 찾습니다.
   */
  private static final Pattern DAY_RANGE_PATTERN =
      Pattern.compile(
          "([월화수목금토일])"
              + "[~∼～-]"
              + "([월화수목금토일])"
      );

  private final UserRepository userRepository;

  private final CollectionAreaScheduleRepository
      collectionAreaScheduleRepository;

  /**
   * 로그인한 일반주택 사용자의
   * 생활쓰레기 / 음식물 / 재활용품
   * 전체 지역 배출 일정을 조회합니다.
   */
  public GeneralHousingScheduleResponse
  getMyGeneralHousingSchedule(
      Long userId
  ) {
    User user =
        getUser(
            userId
        );

    Residence residence =
        getGeneralHousingResidence(
            user
        );

    LocalDateTime referenceDateTime =
        LocalDateTime.now();

    /*
     * Residence에 현재 연결되어 있는
     * 폐기물 종류별 CollectionArea를 Map으로 변환합니다.
     */
    Map<
        CollectionWasteType,
        ResidenceCollectionArea
        > mappingsByWasteType =
        new EnumMap<>(
            CollectionWasteType.class
        );

    for (
        ResidenceCollectionArea mapping
        : residence.getCollectionAreaMappings()
    ) {
      mappingsByWasteType.put(
          mapping.getWasteType(),
          mapping
      );
    }

    /*
     * 연결된 CollectionArea ID를 한 번에 모읍니다.
     */
    List<Long> collectionAreaIds =
        mappingsByWasteType
            .values()
            .stream()
            .map(
                mapping ->
                    mapping
                        .getCollectionArea()
                        .getId()
            )
            .distinct()
            .toList();

    /*
     * 한 Residence에 최대 세 종류의 CollectionArea가
     * 연결될 수 있으므로 일정도 한 번에 조회합니다.
     */
    List<CollectionAreaSchedule> schedules =
        collectionAreaIds.isEmpty()
            ? List.of()
            : collectionAreaScheduleRepository
            .findAllByCollectionAreaIdIn(
                collectionAreaIds
            );

    Map<ScheduleKey, CollectionAreaSchedule>
        schedulesByKey =
        new HashMap<>();

    for (
        CollectionAreaSchedule schedule
        : schedules
    ) {
      schedulesByKey.put(
          new ScheduleKey(
              schedule
                  .getCollectionArea()
                  .getId(),
              schedule.getWasteType()
          ),
          schedule
      );
    }

    List<GeneralHousingWasteScheduleResponse>
        responses =
        new ArrayList<>();

    /*
     * 매칭이 없는 종류도 응답에서 제외하지 않습니다.
     *
     * Flutter가 항상
     * 생활 / 음식물 / 재활용
     * 세 영역을 안정적으로 그릴 수 있도록 합니다.
     */
    for (
        CollectionWasteType wasteType
        : CollectionWasteType.values()
    ) {
      ResidenceCollectionArea mapping =
          mappingsByWasteType.get(
              wasteType
          );

      if (mapping == null) {
        responses.add(
            GeneralHousingWasteScheduleResponse
                .unmatched(
                    wasteType
                )
        );

        continue;
      }

      CollectionArea collectionArea =
          mapping.getCollectionArea();

      CollectionAreaSchedule schedule =
          schedulesByKey.get(
              new ScheduleKey(
                  collectionArea.getId(),
                  wasteType
              )
          );

      if (schedule == null) {
        responses.add(
            GeneralHousingWasteScheduleResponse
                .matchedWithoutSchedule(
                    wasteType,
                    collectionArea
                )
        );

        continue;
      }

      DayParseResult dayParseResult =
          parseEmissionDays(
              schedule.getEmissionDays()
          );

      boolean availableToday =
          isAvailableToday(
              dayParseResult,
              referenceDateTime
                  .toLocalDate()
          );

      boolean availableNow =
          isAvailableNow(
              schedule,
              dayParseResult,
              referenceDateTime
          );

      NextScheduleResult nextSchedule =
          calculateNextSchedule(
              schedule,
              dayParseResult,
              referenceDateTime,
              availableNow
          );

      responses.add(
          GeneralHousingWasteScheduleResponse.of(
              wasteType,
              collectionArea,
              schedule,
              dayParseResult.interpretable(),
              availableToday,
              availableNow,
              nextSchedule
                  .nextAvailableDate(),
              nextSchedule
                  .nextAvailableAt()
          )
      );
    }

    return GeneralHousingScheduleResponse.of(
        residence,
        referenceDateTime,
        responses
    );
  }

  /**
   * 공공데이터의 배출요일 문자열을
   * Java DayOfWeek 집합으로 변환합니다.
   *
   * 지원 예:
   *
   * 일+화+목
   * 월,수,금
   * 월~금
   * 매주 화, 목
   * 매일
   * 평일
   * 주말
   *
   * "주 3회"처럼 정확한 요일이 없는 표현은
   * 억지로 추측하지 않고 interpretable=false로 둡니다.
   */
  private DayParseResult parseEmissionDays(
      String emissionDays
  ) {
    if (
        emissionDays == null
            || emissionDays.isBlank()
    ) {
      return DayParseResult.uninterpretable();
    }

    String normalized =
        emissionDays
            .replaceAll(
                "\\s+",
                ""
            )
            .replace(
                "요일",
                ""
            );

    /*
     * 매일 / 상시는 모든 요일로 해석합니다.
     */
    if (
        normalized.contains("매일")
            || normalized.contains("상시")
    ) {
      return new DayParseResult(
          EnumSet.allOf(
              DayOfWeek.class
          ),
          true
      );
    }

    Set<DayOfWeek> result =
        EnumSet.noneOf(
            DayOfWeek.class
        );

    /*
     * 평일은 월~금입니다.
     */
    if (normalized.contains("평일")) {
      result.add(
          DayOfWeek.MONDAY
      );
      result.add(
          DayOfWeek.TUESDAY
      );
      result.add(
          DayOfWeek.WEDNESDAY
      );
      result.add(
          DayOfWeek.THURSDAY
      );
      result.add(
          DayOfWeek.FRIDAY
      );

      normalized =
          normalized.replace(
              "평일",
              ""
          );
    }

    /*
     * 주말은 토/일입니다.
     */
    if (normalized.contains("주말")) {
      result.add(
          DayOfWeek.SATURDAY
      );
      result.add(
          DayOfWeek.SUNDAY
      );

      normalized =
          normalized.replace(
              "주말",
              ""
          );
    }

    normalized =
        normalized.replace(
            "매주",
            ""
        );

    /*
     * 월~금 같은 범위를 해석합니다.
     */
    Matcher rangeMatcher =
        DAY_RANGE_PATTERN.matcher(
            normalized
        );

    while (rangeMatcher.find()) {
      DayOfWeek startDay =
          toDayOfWeek(
              rangeMatcher.group(1)
          );

      DayOfWeek endDay =
          toDayOfWeek(
              rangeMatcher.group(2)
          );

      addDayRange(
          result,
          startDay,
          endDay
      );
    }

    /*
     * + , / · 등의 구분자를 기준으로
     * 개별 요일을 해석합니다.
     */
    String[] tokens =
        normalized.split(
            "[+,/·ㆍ;|]"
        );

    for (String token : tokens) {
      addDayToken(
          result,
          token
      );
    }

    if (result.isEmpty()) {
      return DayParseResult.uninterpretable();
    }

    return new DayParseResult(
        result,
        true
    );
  }

  /**
   * 단일 요일 또는
   * "월수금"처럼 붙어 있는 표현을 처리합니다.
   *
   * "격일", "주3회"처럼 다른 문자가 섞인 표현은
   * 자동 추측하지 않습니다.
   */
  private void addDayToken(
      Set<DayOfWeek> result,
      String token
  ) {
    if (
        token == null
            || token.isBlank()
    ) {
      return;
    }

    /*
     * 월~금 같은 범위 표현은
     * 앞 단계에서 이미 처리했습니다.
     */
    if (
        token.contains("~")
            || token.contains("∼")
            || token.contains("～")
            || token.contains("-")
    ) {
      return;
    }

    String cleaned =
        token.replaceAll(
            "\\(.*\\)$",
            ""
        );

    /*
     * 정확히 요일 문자만으로 이루어진 경우만
     * 자동 해석합니다.
     *
     * 예:
     * 월
     * 월수금
     * 일화목
     */
    if (
        !cleaned.matches(
            "[월화수목금토일]+"
        )
    ) {
      return;
    }

    for (
        int index = 0;
        index < cleaned.length();
        index++
    ) {
      String dayCharacter =
          String.valueOf(
              cleaned.charAt(index)
          );

      DayOfWeek dayOfWeek =
          toDayOfWeek(
              dayCharacter
          );

      if (dayOfWeek != null) {
        result.add(
            dayOfWeek
        );
      }
    }
  }

  /**
   * 월~금처럼 범위로 작성된 요일을
   * 실제 요일 집합에 추가합니다.
   *
   * 토~월처럼 주 경계를 넘어가는 표현도 처리합니다.
   */
  private void addDayRange(
      Set<DayOfWeek> result,
      DayOfWeek startDay,
      DayOfWeek endDay
  ) {
    if (
        startDay == null
            || endDay == null
    ) {
      return;
    }

    int startIndex =
        WEEK_ORDER.indexOf(
            startDay
        );

    int currentIndex =
        startIndex;

    /*
     * 최대 일주일만 반복하여
     * 잘못된 데이터로 무한 루프가 생기지 않게 합니다.
     */
    for (
        int count = 0;
        count < WEEK_ORDER.size();
        count++
    ) {
      DayOfWeek currentDay =
          WEEK_ORDER.get(
              currentIndex
          );

      result.add(
          currentDay
      );

      if (currentDay == endDay) {
        return;
      }

      currentIndex =
          (
              currentIndex + 1
          )
              % WEEK_ORDER.size();
    }
  }

  /**
   * 한국어 한 글자 요일을
   * Java DayOfWeek로 변환합니다.
   */
  private DayOfWeek toDayOfWeek(
      String value
  ) {
    if (value == null) {
      return null;
    }

    return switch (value) {
      case "월" -> DayOfWeek.MONDAY;
      case "화" -> DayOfWeek.TUESDAY;
      case "수" -> DayOfWeek.WEDNESDAY;
      case "목" -> DayOfWeek.THURSDAY;
      case "금" -> DayOfWeek.FRIDAY;
      case "토" -> DayOfWeek.SATURDAY;
      case "일" -> DayOfWeek.SUNDAY;
      default -> null;
    };
  }

  /**
   * 오늘 시작하는 배출 일정이 존재하는지 확인합니다.
   *
   * 자정을 넘기는 전날 일정이 현재까지 이어지는 것은
   * availableNow에서 별도로 처리합니다.
   */
  private boolean isAvailableToday(
      DayParseResult dayParseResult,
      LocalDate referenceDate
  ) {
    if (!dayParseResult.interpretable()) {
      return false;
    }

    return dayParseResult
        .days()
        .contains(
            referenceDate
                .getDayOfWeek()
        );
  }

  /**
   * 현재 시각에 실제로 배출 가능한지 계산합니다.
   *
   * 일반 일정:
   * 18:00 ~ 22:00
   *
   * 자정 넘김:
   * 20:00 ~ 02:00
   *
   * 수요일 01:00이라면
   * 화요일 20:00에 시작한 일정도 확인합니다.
   */
  private boolean isAvailableNow(
      CollectionAreaSchedule schedule,
      DayParseResult dayParseResult,
      LocalDateTime referenceDateTime
  ) {
    if (!dayParseResult.interpretable()) {
      return false;
    }

    if (!schedule.hasTimeWindow()) {
      return false;
    }

    LocalTime startTime =
        schedule.getStartTime();

    LocalTime endTime =
        schedule.getEndTime();

    /*
     * 시작과 종료가 같은 경우를
     * 24시간 일정이라고 임의로 추측하지 않습니다.
     */
    if (startTime.equals(endTime)) {
      return false;
    }

    LocalTime currentTime =
        referenceDateTime
            .toLocalTime();

    DayOfWeek today =
        referenceDateTime
            .getDayOfWeek();

    /*
     * 18:00 ~ 22:00 같은 일반 일정
     */
    if (!schedule.isOvernight()) {
      if (
          !dayParseResult
              .days()
              .contains(today)
      ) {
        return false;
      }

      return !currentTime.isBefore(
          startTime
      )
          && currentTime.isBefore(
          endTime
      );
    }

    /*
     * 20:00 ~ 02:00 같은 자정 넘김 일정
     *
     * 20:00 이후라면 오늘 시작 일정 확인
     */
    if (
        !currentTime.isBefore(
            startTime
        )
    ) {
      return dayParseResult
          .days()
          .contains(
              today
          );
    }

    /*
     * 02:00 이전이라면
     * 전날 시작된 일정인지 확인
     */
    if (
        currentTime.isBefore(
            endTime
        )
    ) {
      DayOfWeek previousDay =
          today.minus(1);

      return dayParseResult
          .days()
          .contains(
              previousDay
          );
    }

    return false;
  }

  /**
   * 현재 시각 기준으로
   * 가장 가까운 다음 배출 기회를 계산합니다.
   */
  private NextScheduleResult
  calculateNextSchedule(
      CollectionAreaSchedule schedule,
      DayParseResult dayParseResult,
      LocalDateTime referenceDateTime,
      boolean availableNow
  ) {
    if (!dayParseResult.interpretable()) {
      return NextScheduleResult.empty();
    }

    /*
     * 이미 현재 배출 가능한 상태라면
     * 다음 가능 시점을 '현재'로 반환합니다.
     */
    if (availableNow) {
      return new NextScheduleResult(
          referenceDateTime
              .toLocalDate(),
          referenceDateTime
      );
    }

    LocalTime startTime =
        schedule.getStartTime();

    /*
     * 정확한 시작 시간이 없는 경우에는
     * 다음 배출 날짜까지만 계산합니다.
     */
    if (startTime == null) {
      LocalDate nextDate =
          findNextScheduledDate(
              dayParseResult.days(),
              referenceDateTime
                  .toLocalDate()
          );

      return new NextScheduleResult(
          nextDate,
          null
      );
    }

    /*
     * 오늘부터 다음 주 같은 요일까지
     * 최대 7일 뒤까지 확인합니다.
     */
    for (
        int daysLater = 0;
        daysLater <= 7;
        daysLater++
    ) {
      LocalDate candidateDate =
          referenceDateTime
              .toLocalDate()
              .plusDays(
                  daysLater
              );

      if (
          !dayParseResult
              .days()
              .contains(
                  candidateDate
                      .getDayOfWeek()
              )
      ) {
        continue;
      }

      LocalDateTime candidateDateTime =
          candidateDate.atTime(
              startTime
          );

      if (
          !candidateDateTime.isBefore(
              referenceDateTime
          )
      ) {
        return new NextScheduleResult(
            candidateDate,
            candidateDateTime
        );
      }
    }

    return NextScheduleResult.empty();
  }

  /**
   * 시간이 없는 일정의
   * 가장 가까운 다음 요일을 찾습니다.
   */
  private LocalDate findNextScheduledDate(
      Set<DayOfWeek> days,
      LocalDate referenceDate
  ) {
    for (
        int daysLater = 0;
        daysLater <= 7;
        daysLater++
    ) {
      LocalDate candidate =
          referenceDate.plusDays(
              daysLater
          );

      if (
          days.contains(
              candidate
                  .getDayOfWeek()
          )
      ) {
        return candidate;
      }
    }

    return null;
  }

  /**
   * 로그인 사용자를 조회합니다.
   */
  private User getUser(
      Long userId
  ) {
    return userRepository
        .findById(
            userId
        )
        .orElseThrow(
            () ->
                new CustomException(
                    ErrorCode.USER_NOT_FOUND
                )
        );
  }

  /**
   * 일반주택 사용자의 Residence를 확인합니다.
   */
  private Residence getGeneralHousingResidence(
      User user
  ) {
    if (
        user.getResidenceType()
            != ResidenceType.GENERAL_HOUSING
            || user.getResidence() == null
    ) {
      throw new CustomException(
          ErrorCode.USER_RESIDENCE_NOT_SET
      );
    }

    return user.getResidence();
  }

  /**
   * CollectionArea + wasteType을
   * 일정 Map의 Key로 사용합니다.
   */
  private record ScheduleKey(

      Long collectionAreaId,

      CollectionWasteType wasteType

  ) {
  }

  /**
   * 요일 문자열 파싱 결과입니다.
   */
  private record DayParseResult(

      Set<DayOfWeek> days,

      boolean interpretable

  ) {

    private static DayParseResult
    uninterpretable() {
      return new DayParseResult(
          Set.of(),
          false
      );
    }
  }

  /**
   * 다음 배출 가능 날짜/시간 계산 결과입니다.
   */
  private record NextScheduleResult(

      LocalDate nextAvailableDate,

      LocalDateTime nextAvailableAt

  ) {

    private static NextScheduleResult empty() {
      return new NextScheduleResult(
          null,
          null
      );
    }
  }
}