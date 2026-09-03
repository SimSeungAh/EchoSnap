package com.echosnap.backend.domain.schedule.service;

import com.echosnap.backend.domain.collectionarea.entity.CollectionArea;
import com.echosnap.backend.domain.collectionarea.entity.CollectionWasteType;
import com.echosnap.backend.domain.residence.entity.Residence;
import com.echosnap.backend.domain.residence.entity.ResidenceCollectionArea;
import com.echosnap.backend.domain.schedule.dto.response.GeneralHousingScheduleResponse;
import com.echosnap.backend.domain.schedule.dto.response.GeneralHousingWasteScheduleResponse;
import com.echosnap.backend.domain.schedule.entity.CollectionAreaSchedule;
import com.echosnap.backend.domain.schedule.entity.ScheduleException;
import com.echosnap.backend.domain.schedule.repository.CollectionAreaScheduleRepository;
import com.echosnap.backend.domain.schedule.repository.ScheduleExceptionRepository;
import com.echosnap.backend.domain.user.entity.ResidenceType;
import com.echosnap.backend.domain.user.entity.User;
import com.echosnap.backend.domain.user.repository.UserRepository;
import com.echosnap.backend.global.exception.CustomException;
import com.echosnap.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CollectionAreaScheduleQueryService {

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

  private static final Pattern DAY_RANGE_PATTERN =
      Pattern.compile(
          "([월화수목금토일])"
              + "[~∼～-]"
              + "([월화수목금토일])"
      );

  private final UserRepository userRepository;

  private final CollectionAreaScheduleRepository
      collectionAreaScheduleRepository;

  private final ScheduleExceptionRepository
      scheduleExceptionRepository;

  /**
   * 로그인한 일반주택 사용자의
   * 생활쓰레기 / 음식물 / 재활용품
   * 전체 일정을 조회합니다.
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
     * Residence에 연결된 CollectionArea ID를
     * 중복 없이 모읍니다.
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
     * 정기 일정 한 번에 조회
     */
    List<CollectionAreaSchedule> schedules =
        collectionAreaIds.isEmpty()
            ? List.of()
            : collectionAreaScheduleRepository
            .findAllByCollectionAreaIdIn(
                collectionAreaIds
            );

    /*
     * 전날 시작한 overnight 예외가
     * 오늘 새벽까지 이어질 수 있으므로
     * 어제 날짜부터 예외를 조회합니다.
     */
    LocalDate exceptionStartDate =
        referenceDateTime
            .toLocalDate()
            .minusDays(1);

    /*
     * 일반주택의 최대 세 수거구역에 대한
     * 예외를 한 번에 조회합니다.
     */
    List<ScheduleException> exceptions =
        collectionAreaIds.isEmpty()
            ? List.of()
            : scheduleExceptionRepository
            .findAllByCollectionAreaIdInAndEffectiveDateGreaterThanEqualOrderByEffectiveDateAsc(
                collectionAreaIds,
                exceptionStartDate
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

    /*
     * CollectionArea + CollectionWasteType별로
     * ScheduleException을 그룹화합니다.
     */
    Map<ScheduleKey, List<ScheduleException>>
        exceptionsByKey =
        new HashMap<>();

    for (
        ScheduleException exception
        : exceptions
    ) {
      if (
          exception.getCollectionArea() == null
              || exception.getCollectionWasteType()
              == null
      ) {
        continue;
      }

      ScheduleKey key =
          new ScheduleKey(
              exception
                  .getCollectionArea()
                  .getId(),
              exception
                  .getCollectionWasteType()
          );

      exceptionsByKey
          .computeIfAbsent(
              key,
              ignored ->
                  new ArrayList<>()
          )
          .add(
              exception
          );
    }

    List<GeneralHousingWasteScheduleResponse>
        responses =
        new ArrayList<>();

    /*
     * 매칭되지 않은 폐기물 종류도
     * 응답에서 제외하지 않습니다.
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

      ScheduleKey key =
          new ScheduleKey(
              collectionArea.getId(),
              wasteType
          );

      CollectionAreaSchedule schedule =
          schedulesByKey.get(
              key
          );

      List<ScheduleException> wasteExceptions =
          exceptionsByKey.getOrDefault(
              key,
              List.of()
          );

      ScheduleException todayException =
          findException(
              wasteExceptions,
              referenceDateTime
                  .toLocalDate()
          );

      /*
       * 정기 일정이 없더라도
       * 특정 날짜의 공식 예외 정보가 존재하면
       * 그 예외 자체는 사용자에게 보여줄 수 있습니다.
       */
      if (schedule == null) {
        ExceptionOnlyResult exceptionResult =
            calculateExceptionOnlyResult(
                wasteExceptions,
                referenceDateTime
            );

        responses.add(
            GeneralHousingWasteScheduleResponse
                .matchedWithoutSchedule(
                    wasteType,
                    collectionArea,
                    exceptionResult
                        .availableToday(),
                    exceptionResult
                        .availableNow(),
                    exceptionResult
                        .nextAvailableDate(),
                    exceptionResult
                        .nextAvailableAt(),
                    todayException
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
              todayException,
              referenceDateTime
                  .toLocalDate()
          );

      boolean availableNow =
          isAvailableNow(
              schedule,
              dayParseResult,
              wasteExceptions,
              referenceDateTime
          );

      NextScheduleResult nextSchedule =
          calculateNextSchedule(
              schedule,
              dayParseResult,
              wasteExceptions,
              referenceDateTime,
              availableNow
          );

      responses.add(
          GeneralHousingWasteScheduleResponse.of(
              wasteType,
              collectionArea,
              schedule,
              dayParseResult
                  .interpretable(),
              availableToday,
              availableNow,
              nextSchedule
                  .nextAvailableDate(),
              nextSchedule
                  .nextAvailableAt(),
              todayException
          )
      );
    }

    return GeneralHousingScheduleResponse.of(
        residence,
        referenceDateTime,
        responses
    );
  }

  /*
   * =========================================================
   * 요일 문자열 파싱
   * =========================================================
   */

  private DayParseResult parseEmissionDays(
      String emissionDays
  ) {
    if (
        emissionDays == null
            || emissionDays.isBlank()
    ) {
      return DayParseResult
          .uninterpretable();
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
      return DayParseResult
          .uninterpretable();
    }

    return new DayParseResult(
        result,
        true
    );
  }

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
     * 주3회, 격일처럼
     * 정확한 요일을 알 수 없는 값은
     * 추측하지 않습니다.
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

  /*
   * =========================================================
   * 오늘 일정 계산
   * =========================================================
   */

  /**
   * 오늘 ScheduleException이 존재하면
   * 반복 일정보다 무조건 우선합니다.
   */
  private boolean isAvailableToday(
      DayParseResult dayParseResult,
      ScheduleException todayException,
      LocalDate referenceDate
  ) {
    if (todayException != null) {
      return isExceptionAvailableOnDate(
          todayException
      );
    }

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
   * 현재 배출 가능 여부입니다.
   *
   * 우선순위:
   *
   * 1. 오늘 ScheduleException
   * 2. 전날 ScheduleException의 overnight 연장
   * 3. 오늘 시작한 정기 일정
   * 4. 전날 시작한 정기 overnight 일정
   */
  private boolean isAvailableNow(
      CollectionAreaSchedule schedule,
      DayParseResult dayParseResult,
      List<ScheduleException> exceptions,
      LocalDateTime referenceDateTime
  ) {
    LocalDate today =
        referenceDateTime
            .toLocalDate();

    LocalTime currentTime =
        referenceDateTime
            .toLocalTime();

    ScheduleException todayException =
        findException(
            exceptions,
            today
        );

    ScheduleException yesterdayException =
        findException(
            exceptions,
            today.minusDays(1)
        );

    /*
     * 오늘 예외가 존재하면
     * 오늘에 적용되는 정기 일정 전체를 덮어씁니다.
     */
    if (todayException != null) {
      return isExceptionAvailableNowOnEffectiveDate(
          todayException,
          currentTime
      );
    }

    boolean currentDayRegular =
        isCurrentDayRegularAvailableNow(
            schedule,
            dayParseResult,
            referenceDateTime
        );

    boolean previousDayContinuation;

    /*
     * 전날 ScheduleException이 있었다면
     * 전날 정기 일정은 이미 예외에 의해 덮어써졌습니다.
     *
     * 따라서 전날 regular overnight을 같이 적용하면 안 됩니다.
     */
    if (yesterdayException != null) {
      previousDayContinuation =
          isExceptionOvernightContinuationAvailable(
              yesterdayException,
              currentTime
          );
    } else {
      previousDayContinuation =
          isRegularOvernightContinuationAvailable(
              schedule,
              dayParseResult,
              referenceDateTime
          );
    }

    return currentDayRegular
        || previousDayContinuation;
  }

  /**
   * 오늘 시작한 정기 일정의
   * 현재 시간 범위를 확인합니다.
   */
  private boolean isCurrentDayRegularAvailableNow(
      CollectionAreaSchedule schedule,
      DayParseResult dayParseResult,
      LocalDateTime referenceDateTime
  ) {
    if (
        !dayParseResult.interpretable()
            || !schedule.hasTimeWindow()
    ) {
      return false;
    }

    LocalTime startTime =
        schedule.getStartTime();

    LocalTime endTime =
        schedule.getEndTime();

    if (startTime.equals(endTime)) {
      return false;
    }

    DayOfWeek today =
        referenceDateTime
            .getDayOfWeek();

    if (
        !dayParseResult
            .days()
            .contains(
                today
            )
    ) {
      return false;
    }

    LocalTime currentTime =
        referenceDateTime
            .toLocalTime();

    if (schedule.isOvernight()) {
      /*
       * 20:00~02:00의 오늘 시작 구간은
       * 20:00 이후입니다.
       *
       * 02:00 이전은 전날 시작 일정에서 처리합니다.
       */
      return !currentTime.isBefore(
          startTime
      );
    }

    return !currentTime.isBefore(
        startTime
    )
        && currentTime.isBefore(
        endTime
    );
  }

  /**
   * 전날 시작한 정기 overnight 일정이
   * 오늘 새벽까지 이어지는지 확인합니다.
   */
  private boolean isRegularOvernightContinuationAvailable(
      CollectionAreaSchedule schedule,
      DayParseResult dayParseResult,
      LocalDateTime referenceDateTime
  ) {
    if (
        !dayParseResult.interpretable()
            || !schedule.hasTimeWindow()
            || !schedule.isOvernight()
    ) {
      return false;
    }

    LocalTime currentTime =
        referenceDateTime
            .toLocalTime();

    if (
        !currentTime.isBefore(
            schedule.getEndTime()
        )
    ) {
      return false;
    }

    DayOfWeek previousDay =
        referenceDateTime
            .getDayOfWeek()
            .minus(1);

    return dayParseResult
        .days()
        .contains(
            previousDay
        );
  }

  /*
   * =========================================================
   * ScheduleException 계산
   * =========================================================
   */

  private boolean isExceptionAvailableOnDate(
      ScheduleException exception
  ) {
    if (exception.isUnavailable()) {
      return false;
    }

    return exception.hasTimeWindow()
        || Boolean.TRUE.equals(
        exception.getAlwaysAvailable()
    );
  }

  /**
   * effectiveDate 당일의 예외 시간입니다.
   */
  private boolean
  isExceptionAvailableNowOnEffectiveDate(
      ScheduleException exception,
      LocalTime currentTime
  ) {
    if (exception.isUnavailable()) {
      return false;
    }

    if (
        Boolean.TRUE.equals(
            exception.getAlwaysAvailable()
        )
    ) {
      return true;
    }

    if (!exception.hasTimeWindow()) {
      return false;
    }

    if (exception.isOvernight()) {
      return !currentTime.isBefore(
          exception.getStartTime()
      );
    }

    return !currentTime.isBefore(
        exception.getStartTime()
    )
        && currentTime.isBefore(
        exception.getEndTime()
    );
  }

  /**
   * 전날 예외가
   * 오늘 새벽까지 이어지는 경우입니다.
   */
  private boolean
  isExceptionOvernightContinuationAvailable(
      ScheduleException exception,
      LocalTime currentTime
  ) {
    if (
        exception.isUnavailable()
            || !exception.isOvernight()
    ) {
      return false;
    }

    return currentTime.isBefore(
        exception.getEndTime()
    );
  }

  /*
   * =========================================================
   * 다음 일정 계산
   * =========================================================
   */

  private NextScheduleResult
  calculateNextSchedule(
      CollectionAreaSchedule schedule,
      DayParseResult dayParseResult,
      List<ScheduleException> exceptions,
      LocalDateTime referenceDateTime,
      boolean availableNow
  ) {
    /*
     * 지금 배출 가능한 상태면
     * 가장 가까운 가능 시점은 현재입니다.
     */
    if (availableNow) {
      return new NextScheduleResult(
          referenceDateTime
              .toLocalDate(),
          referenceDateTime
      );
    }

    Map<LocalDate, ScheduleException>
        exceptionByDate =
        new HashMap<>();

    for (
        ScheduleException exception
        : exceptions
    ) {
      exceptionByDate.put(
          exception.getEffectiveDate(),
          exception
      );
    }

    LocalDate referenceDate =
        referenceDateTime
            .toLocalDate();

    /*
     * 기본적으로 반복 규칙은 7일 안에 다시 옵니다.
     */
    LocalDate searchEndDate =
        referenceDate.plusDays(7);

    /*
     * 미래 예외가 여러 주 연속 존재할 수 있으므로
     * 마지막 예외 날짜 + 7일까지 확인합니다.
     */
    LocalDate lastExceptionDate =
        exceptions.stream()
            .map(
                ScheduleException::getEffectiveDate
            )
            .filter(
                date ->
                    !date.isBefore(
                        referenceDate
                    )
            )
            .max(
                Comparator.naturalOrder()
            )
            .orElse(
                null
            );

    if (lastExceptionDate != null) {
      LocalDate afterLastException =
          lastExceptionDate.plusDays(7);

      if (
          afterLastException.isAfter(
              searchEndDate
          )
      ) {
        searchEndDate =
            afterLastException;
      }
    }

    LocalDate candidateDate =
        referenceDate;

    while (
        !candidateDate.isAfter(
            searchEndDate
        )
    ) {
      ScheduleException exception =
          exceptionByDate.get(
              candidateDate
          );

      DayScheduleCandidate candidate;

      if (exception != null) {
        /*
         * 예외가 있는 날짜에는
         * 정기 일정을 절대로 함께 적용하지 않습니다.
         */
        candidate =
            buildExceptionCandidate(
                exception,
                candidateDate,
                referenceDateTime
            );
      } else {
        candidate =
            buildRegularCandidate(
                schedule,
                dayParseResult,
                candidateDate,
                referenceDateTime
            );
      }

      if (candidate.available()) {
        return new NextScheduleResult(
            candidateDate,
            candidate.availableAt()
        );
      }

      candidateDate =
          candidateDate.plusDays(1);
    }

    return NextScheduleResult.empty();
  }

  /**
   * 특정 날짜 예외를
   * 다음 배출 후보로 변환합니다.
   */
  private DayScheduleCandidate
  buildExceptionCandidate(
      ScheduleException exception,
      LocalDate candidateDate,
      LocalDateTime referenceDateTime
  ) {
    if (exception.isUnavailable()) {
      return DayScheduleCandidate
          .unavailable();
    }

    if (
        Boolean.TRUE.equals(
            exception.getAlwaysAvailable()
        )
    ) {
      return DayScheduleCandidate
          .available(
              null
          );
    }

    if (!exception.hasTimeWindow()) {
      return DayScheduleCandidate
          .unavailable();
    }

    LocalDateTime startDateTime =
        candidateDate.atTime(
            exception.getStartTime()
        );

    /*
     * 오늘 일정인데 시작 시간이 이미 지났더라도
     * availableNow라면 위에서 현재 시각으로 이미 반환됐습니다.
     *
     * availableNow가 false인데 시작까지 지났다면
     * 오늘 이 예외 일정의 기회는 끝난 것입니다.
     */
    if (
        candidateDate.equals(
            referenceDateTime.toLocalDate()
        )
            && startDateTime.isBefore(
            referenceDateTime
        )
    ) {
      /*
       * overnight 일정의 경우
       * 새벽 이후 오늘 저녁에 다시 시작하는
       * effectiveDate 일정은 startDateTime이
       * 아직 미래라 여기에 걸리지 않습니다.
       */
      return DayScheduleCandidate
          .unavailable();
    }

    return DayScheduleCandidate
        .available(
            startDateTime
        );
  }

  /**
   * 정기 CollectionAreaSchedule을
   * 특정 날짜 후보로 변환합니다.
   */
  private DayScheduleCandidate
  buildRegularCandidate(
      CollectionAreaSchedule schedule,
      DayParseResult dayParseResult,
      LocalDate candidateDate,
      LocalDateTime referenceDateTime
  ) {
    if (!dayParseResult.interpretable()) {
      return DayScheduleCandidate
          .unavailable();
    }

    if (
        !dayParseResult
            .days()
            .contains(
                candidateDate
                    .getDayOfWeek()
            )
    ) {
      return DayScheduleCandidate
          .unavailable();
    }

    LocalTime startTime =
        schedule.getStartTime();

    /*
     * 공공데이터에 시작 시간이 없는 경우에도
     * 배출 요일 자체는 제공할 수 있습니다.
     */
    if (startTime == null) {
      return DayScheduleCandidate
          .available(
              null
          );
    }

    LocalDateTime candidateDateTime =
        candidateDate.atTime(
            startTime
        );

    if (
        candidateDate.equals(
            referenceDateTime.toLocalDate()
        )
            && candidateDateTime.isBefore(
            referenceDateTime
        )
    ) {
      /*
       * 현재 배출 가능 상태였다면
       * calculateNextSchedule() 시작 부분에서
       * 이미 현재 시각을 반환했으므로,
       *
       * 여기까지 왔다는 것은 오늘 시작 시각을
       * 다시 다음 일정으로 잡으면 안 된다는 뜻입니다.
       */
      return DayScheduleCandidate
          .unavailable();
    }

    return DayScheduleCandidate
        .available(
            candidateDateTime
        );
  }

  /*
   * =========================================================
   * 정기 일정이 없는 경우의 예외 계산
   * =========================================================
   */

  private ExceptionOnlyResult
  calculateExceptionOnlyResult(
      List<ScheduleException> exceptions,
      LocalDateTime referenceDateTime
  ) {
    if (exceptions.isEmpty()) {
      return ExceptionOnlyResult.empty();
    }

    LocalDate today =
        referenceDateTime
            .toLocalDate();

    LocalTime currentTime =
        referenceDateTime
            .toLocalTime();

    ScheduleException todayException =
        findException(
            exceptions,
            today
        );

    ScheduleException yesterdayException =
        findException(
            exceptions,
            today.minusDays(1)
        );

    boolean availableToday =
        todayException != null
            && isExceptionAvailableOnDate(
            todayException
        );

    boolean availableNow;

    if (todayException != null) {
      availableNow =
          isExceptionAvailableNowOnEffectiveDate(
              todayException,
              currentTime
          );
    } else {
      availableNow =
          yesterdayException != null
              && isExceptionOvernightContinuationAvailable(
              yesterdayException,
              currentTime
          );
    }

    if (availableNow) {
      return new ExceptionOnlyResult(
          availableToday,
          true,
          today,
          referenceDateTime
      );
    }

    ScheduleException nextException =
        exceptions.stream()
            .filter(
                exception ->
                    !exception
                        .getEffectiveDate()
                        .isBefore(
                            today
                        )
            )
            .filter(
                exception ->
                    !exception.isUnavailable()
            )
            .filter(
                ScheduleException::hasTimeWindow
            )
            .filter(
                exception -> {
                  LocalDateTime start =
                      exception
                          .getEffectiveDate()
                          .atTime(
                              exception
                                  .getStartTime()
                          );

                  return !start.isBefore(
                      referenceDateTime
                  );
                }
            )
            .min(
                Comparator.comparing(
                    exception ->
                        exception
                            .getEffectiveDate()
                            .atTime(
                                exception
                                    .getStartTime()
                            )
                )
            )
            .orElse(
                null
            );

    if (nextException == null) {
      return new ExceptionOnlyResult(
          availableToday,
          false,
          null,
          null
      );
    }

    return new ExceptionOnlyResult(
        availableToday,
        false,
        nextException.getEffectiveDate(),
        nextException
            .getEffectiveDate()
            .atTime(
                nextException
                    .getStartTime()
            )
    );
  }

  /*
   * =========================================================
   * 공통
   * =========================================================
   */

  private ScheduleException findException(
      List<ScheduleException> exceptions,
      LocalDate date
  ) {
    return exceptions.stream()
        .filter(
            exception ->
                Objects.equals(
                    exception
                        .getEffectiveDate(),
                    date
                )
        )
        .findFirst()
        .orElse(
            null
        );
  }

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

  private record ScheduleKey(

      Long collectionAreaId,

      CollectionWasteType wasteType

  ) {
  }

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

  private record DayScheduleCandidate(

      boolean available,

      LocalDateTime availableAt

  ) {

    private static DayScheduleCandidate available(
        LocalDateTime availableAt
    ) {
      return new DayScheduleCandidate(
          true,
          availableAt
      );
    }

    private static DayScheduleCandidate unavailable() {
      return new DayScheduleCandidate(
          false,
          null
      );
    }
  }

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

  private record ExceptionOnlyResult(

      boolean availableToday,

      boolean availableNow,

      LocalDate nextAvailableDate,

      LocalDateTime nextAvailableAt

  ) {

    private static ExceptionOnlyResult empty() {
      return new ExceptionOnlyResult(
          false,
          false,
          null,
          null
      );
    }
  }
}