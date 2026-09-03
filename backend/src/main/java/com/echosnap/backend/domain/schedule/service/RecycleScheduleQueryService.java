package com.echosnap.backend.domain.schedule.service;

import com.echosnap.backend.domain.apartment.entity.Apartment;
import com.echosnap.backend.domain.apartment.entity.ApartmentStatus;
import com.echosnap.backend.domain.apartment.repository.ApartmentRepository;
import com.echosnap.backend.domain.schedule.dto.response.ApartmentScheduleResponse;
import com.echosnap.backend.domain.schedule.dto.response.RecycleScheduleResponse;
import com.echosnap.backend.domain.schedule.dto.response.RecycleScheduleTimeResponse;
import com.echosnap.backend.domain.schedule.dto.response.WasteItemScheduleResponse;
import com.echosnap.backend.domain.schedule.entity.RecycleSchedule;
import com.echosnap.backend.domain.schedule.entity.ScheduleException;
import com.echosnap.backend.domain.schedule.repository.RecycleScheduleRepository;
import com.echosnap.backend.domain.schedule.repository.ScheduleExceptionRepository;
import com.echosnap.backend.domain.user.entity.User;
import com.echosnap.backend.domain.user.repository.UserRepository;
import com.echosnap.backend.domain.waste.entity.WasteItem;
import com.echosnap.backend.domain.waste.repository.WasteItemRepository;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecycleScheduleQueryService {

    private final RecycleScheduleRepository
        recycleScheduleRepository;

    private final ScheduleExceptionRepository
        scheduleExceptionRepository;

    private final ApartmentRepository
        apartmentRepository;

    private final WasteItemRepository
        wasteItemRepository;

    private final UserRepository
        userRepository;

    /**
     * 배출 일정 하나를 상세 조회합니다.
     *
     * 관리자 일정 상세 화면에서 사용합니다.
     */
    public RecycleScheduleResponse getSchedule(
        Long scheduleId
    ) {
        RecycleSchedule schedule =
            recycleScheduleRepository
                .findDetailById(
                    scheduleId
                )
                .orElseThrow(
                    () ->
                        new CustomException(
                            ErrorCode.RECYCLE_SCHEDULE_NOT_FOUND
                        )
                );

        return RecycleScheduleResponse.from(
            schedule
        );
    }

    /**
     * 특정 아파트에 등록된
     * 공식 반복 배출 일정 전체를 조회합니다.
     *
     * 관리자 일정 목록에서 사용합니다.
     *
     * 이 API는 반복 공식 일정 자체를 관리하기 위한 것이므로
     * ScheduleException을 섞지 않습니다.
     */
    public List<RecycleScheduleResponse>
    getSchedulesByApartment(
        Long apartmentId
    ) {
        getApprovedApartment(
            apartmentId
        );

        return recycleScheduleRepository
            .findAllByApartmentId(
                apartmentId
            )
            .stream()
            .map(
                RecycleScheduleResponse::from
            )
            .toList();
    }

    /**
     * 로그인 사용자의 거주 아파트 기준
     * 전체 배출 일정을 조회합니다.
     */
    public ApartmentScheduleResponse
    getMyApartmentSchedule(
        Long userId
    ) {
        User user =
            getUser(
                userId
            );

        Apartment apartment =
            getUserApartment(
                user
            );

        LocalDateTime referenceDateTime =
            LocalDateTime.now();

        return buildApartmentScheduleResponse(
            apartment,
            referenceDateTime
        );
    }

    /**
     * 로그인 사용자의 거주 아파트 기준
     * 특정 폐기물 품목의 일정을 조회합니다.
     */
    public WasteItemScheduleResponse
    getMyWasteItemSchedule(
        Long userId,
        Long wasteItemId
    ) {
        User user =
            getUser(
                userId
            );

        Apartment apartment =
            getUserApartment(
                user
            );

        WasteItem wasteItem =
            getActiveWasteItem(
                wasteItemId
            );

        List<RecycleSchedule> schedules =
            recycleScheduleRepository
                .findAllByApartmentIdAndWasteItemId(
                    apartment.getId(),
                    wasteItem.getId()
                );

        LocalDateTime referenceDateTime =
            LocalDateTime.now();

        List<ScheduleException> exceptions =
            getWasteItemExceptions(
                apartment.getId(),
                wasteItem.getId(),
                referenceDateTime.toLocalDate()
            );

        return buildWasteItemScheduleResponse(
            wasteItem,
            schedules,
            exceptions,
            referenceDateTime
        );
    }

    /**
     * 지정된 아파트와 폐기물 품목을 기준으로
     * 사용자용 일정을 계산합니다.
     *
     * 폐기물 가이드에 일정 정보를 결합할 때도
     * 내부적으로 재사용할 수 있습니다.
     */
    public WasteItemScheduleResponse
    getWasteItemSchedule(
        Long apartmentId,
        Long wasteItemId
    ) {
        Apartment apartment =
            getApprovedApartment(
                apartmentId
            );

        WasteItem wasteItem =
            getActiveWasteItem(
                wasteItemId
            );

        List<RecycleSchedule> schedules =
            recycleScheduleRepository
                .findAllByApartmentIdAndWasteItemId(
                    apartment.getId(),
                    wasteItem.getId()
                );

        LocalDateTime referenceDateTime =
            LocalDateTime.now();

        List<ScheduleException> exceptions =
            getWasteItemExceptions(
                apartment.getId(),
                wasteItem.getId(),
                referenceDateTime.toLocalDate()
            );

        return buildWasteItemScheduleResponse(
            wasteItem,
            schedules,
            exceptions,
            referenceDateTime
        );
    }

    /**
     * 아파트의 전체 일정을 품목별로 묶어 계산합니다.
     *
     * 반복 일정뿐 아니라
     * 예외 일정만 존재하는 품목도 사용자 화면에
     * 표시할 수 있도록 두 데이터를 함께 묶습니다.
     */
    private ApartmentScheduleResponse
    buildApartmentScheduleResponse(
        Apartment apartment,
        LocalDateTime referenceDateTime
    ) {
        List<RecycleSchedule> schedules =
            recycleScheduleRepository
                .findAllByApartmentId(
                    apartment.getId()
                );

        /*
         * 어제 시작한 overnight 예외가
         * 오늘 새벽까지 이어질 가능성까지 고려하기 위해
         * 하루 전부터 조회합니다.
         *
         * 현재 공동주택 주민 제보 검증에서는
         * overnight을 허용하지 않지만,
         * 데이터 계산 로직은 조금 더 방어적으로 처리합니다.
         */
        LocalDate exceptionStartDate =
            referenceDateTime
                .toLocalDate()
                .minusDays(1);

        List<ScheduleException> exceptions =
            scheduleExceptionRepository
                .findAllByApartmentIdAndEffectiveDateGreaterThanEqualOrderByEffectiveDateAsc(
                    apartment.getId(),
                    exceptionStartDate
                );

        Map<Long, List<RecycleSchedule>>
            schedulesByWasteItem =
            schedules.stream()
                .collect(
                    Collectors.groupingBy(
                        schedule ->
                            schedule
                                .getWasteItem()
                                .getId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                    )
                );

        Map<Long, List<ScheduleException>>
            exceptionsByWasteItem =
            exceptions.stream()
                .filter(
                    exception ->
                        exception.getWasteItem()
                            != null
                )
                .collect(
                    Collectors.groupingBy(
                        exception ->
                            exception
                                .getWasteItem()
                                .getId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                    )
                );

        /*
         * 정기 일정이 없는 품목이라도
         * 특정 날짜의 공식 예외 일정이 존재할 수 있으므로
         * 품목 목록을 합칩니다.
         */
        Map<Long, WasteItem> wasteItems =
            new LinkedHashMap<>();

        for (RecycleSchedule schedule : schedules) {
            wasteItems.putIfAbsent(
                schedule.getWasteItem()
                    .getId(),
                schedule.getWasteItem()
            );
        }

        for (ScheduleException exception : exceptions) {
            if (exception.getWasteItem() != null) {
                wasteItems.putIfAbsent(
                    exception.getWasteItem()
                        .getId(),
                    exception.getWasteItem()
                );
            }
        }

        List<WasteItemScheduleResponse> items =
            new ArrayList<>();

        for (
            Map.Entry<Long, WasteItem> entry
            : wasteItems.entrySet()
        ) {
            Long wasteItemId =
                entry.getKey();

            WasteItem wasteItem =
                entry.getValue();

            List<RecycleSchedule> itemSchedules =
                schedulesByWasteItem
                    .getOrDefault(
                        wasteItemId,
                        List.of()
                    );

            List<ScheduleException> itemExceptions =
                exceptionsByWasteItem
                    .getOrDefault(
                        wasteItemId,
                        List.of()
                    );

            items.add(
                buildWasteItemScheduleResponse(
                    wasteItem,
                    itemSchedules,
                    itemExceptions,
                    referenceDateTime
                )
            );
        }

        return ApartmentScheduleResponse.of(
            apartment,
            referenceDateTime,
            items
        );
    }

    /**
     * 한 품목에 대해
     * 정기 일정 + 특정 날짜 예외 일정을 결합하여
     * 사용자용 계산 결과를 만듭니다.
     */
    private WasteItemScheduleResponse
    buildWasteItemScheduleResponse(
        WasteItem wasteItem,
        List<RecycleSchedule> schedules,
        List<ScheduleException> exceptions,
        LocalDateTime referenceDateTime
    ) {
        LocalDate referenceDate =
            referenceDateTime.toLocalDate();

        ScheduleException todayException =
            findException(
                exceptions,
                referenceDate
            );

        ScheduleException yesterdayException =
            findException(
                exceptions,
                referenceDate.minusDays(1)
            );

        boolean availableToday =
            isAvailableToday(
                schedules,
                todayException,
                referenceDate
            );

        boolean availableNow =
            isAvailableNow(
                schedules,
                todayException,
                yesterdayException,
                referenceDateTime
            );

        NextScheduleResult nextSchedule =
            calculateNextSchedule(
                schedules,
                exceptions,
                referenceDateTime
            );

        List<RecycleScheduleTimeResponse>
            scheduleResponses =
            schedules.stream()
                .map(
                    RecycleScheduleTimeResponse::from
                )
                .toList();

        return WasteItemScheduleResponse.of(
            wasteItem,
            availableToday,
            availableNow,
            nextSchedule.nextAvailableDate(),
            nextSchedule.nextAvailableAt(),
            scheduleResponses,
            todayException
        );
    }

    /**
     * 오늘 배출 일정이 존재하는지 확인합니다.
     *
     * 오늘 ScheduleException이 있으면
     * 정기 일정을 보지 않고 예외만 사용합니다.
     */
    private boolean isAvailableToday(
        List<RecycleSchedule> schedules,
        ScheduleException todayException,
        LocalDate referenceDate
    ) {
        if (todayException != null) {
            return isExceptionAvailableOnDate(
                todayException
            );
        }

        DayOfWeek today =
            referenceDate.getDayOfWeek();

        return schedules.stream()
            .anyMatch(
                schedule ->
                    schedule.isAlwaysAvailable()
                        || schedule.getDayOfWeek()
                        == today
            );
    }

    /**
     * 현재 시각에 실제 배출할 수 있는지 계산합니다.
     *
     * 우선순위:
     *
     * 1. 오늘 ScheduleException
     * 2. 어제 시작한 overnight 예외
     * 3. 반복 RecycleSchedule
     */
    private boolean isAvailableNow(
        List<RecycleSchedule> schedules,
        ScheduleException todayException,
        ScheduleException yesterdayException,
        LocalDateTime referenceDateTime
    ) {
        LocalTime currentTime =
            referenceDateTime.toLocalTime();

        /*
         * 오늘 예외가 존재한다면
         * 오늘의 정기 일정 전체를 덮어씁니다.
         */
        if (todayException != null) {
            return isExceptionAvailableNowOnEffectiveDate(
                todayException,
                currentTime
            );
        }

        /*
         * 어제의 예외 일정이 자정을 넘어
         * 현재 시각까지 이어지는 경우입니다.
         */
        if (
            yesterdayException != null
                && isOvernightContinuationAvailable(
                yesterdayException,
                currentTime
            )
        ) {
            return true;
        }

        return isRegularScheduleAvailableNow(
            schedules,
            referenceDateTime
        );
    }

    /**
     * 예외 일정이 해당 날짜에
     * 배출 가능한 형태인지 확인합니다.
     */
    private boolean isExceptionAvailableOnDate(
        ScheduleException exception
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

        return exception.hasTimeWindow();
    }

    /**
     * 예외 일정의 effectiveDate 당일에
     * 현재 실제 배출 가능한지 확인합니다.
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

        LocalTime startTime =
            exception.getStartTime();

        LocalTime endTime =
            exception.getEndTime();

        if (exception.isOvernight()) {
            /*
             * effectiveDate 당일에는
             * 시작 시간 이후 구간만 해당합니다.
             *
             * 자정 이후 구간은 다음 날
             * yesterdayException 계산에서 처리합니다.
             */
            return !currentTime.isBefore(
                startTime
            );
        }

        return !currentTime.isBefore(startTime)
            && currentTime.isBefore(endTime);
    }

    /**
     * 전날 시작한 overnight ScheduleException이
     * 오늘 새벽까지 이어지는지 확인합니다.
     */
    private boolean
    isOvernightContinuationAvailable(
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

    /**
     * ScheduleException이 없는 경우
     * 기존 반복 일정으로 현재 배출 가능 여부를 계산합니다.
     */
    private boolean isRegularScheduleAvailableNow(
        List<RecycleSchedule> schedules,
        LocalDateTime referenceDateTime
    ) {
        DayOfWeek today =
            referenceDateTime.getDayOfWeek();

        LocalTime currentTime =
            referenceDateTime.toLocalTime();

        return schedules.stream()
            .anyMatch(
                schedule -> {

                    if (
                        schedule.isAlwaysAvailable()
                    ) {
                        return true;
                    }

                    if (
                        schedule.getDayOfWeek()
                            != today
                    ) {
                        return false;
                    }

                    LocalTime startTime =
                        schedule.getStartTime();

                    LocalTime endTime =
                        schedule.getEndTime();

                    boolean started =
                        !currentTime.isBefore(
                            startTime
                        );

                    boolean notEnded =
                        currentTime.isBefore(
                            endTime
                        );

                    return started
                        && notEnded;
                }
            );
    }

    /**
     * 현재 시각 기준
     * 가장 가까운 배출 가능 일정을 계산합니다.
     *
     * 특정 날짜에 ScheduleException이 존재하면
     * 그 날짜의 정기 일정은 완전히 무시하고
     * 예외 일정만 사용합니다.
     */
    private NextScheduleResult
    calculateNextSchedule(
        List<RecycleSchedule> schedules,
        List<ScheduleException> exceptions,
        LocalDateTime referenceDateTime
    ) {
        if (
            schedules.isEmpty()
                && exceptions.isEmpty()
        ) {
            return NextScheduleResult.empty();
        }

        LocalDate referenceDate =
            referenceDateTime.toLocalDate();

        /*
         * 날짜별 예외를 빠르게 찾기 위한 Map입니다.
         *
         * DB Unique Constraint 때문에
         * 같은 품목 + 날짜에는 하나의 예외만 존재합니다.
         */
        Map<LocalDate, ScheduleException>
            exceptionByDate =
            new HashMap<>();

        for (ScheduleException exception : exceptions) {
            exceptionByDate.put(
                exception.getEffectiveDate(),
                exception
            );
        }

        /*
         * 반복 일정은 최대 7일 안에 다시 나타납니다.
         *
         * 미래 예외가 존재한다면
         * 마지막 예외 날짜 이후 7일까지 확인하면
         * 모든 예외를 지나친 다음의 정기 일정도 찾을 수 있습니다.
         */
        LocalDate searchEndDate =
            referenceDate.plusDays(7);

        LocalDate lastExceptionDate =
            exceptions.stream()
                .map(
                    ScheduleException::getEffectiveDate
                )
                .max(
                    Comparator.naturalOrder()
                )
                .orElse(
                    null
                );

        if (
            lastExceptionDate != null
                && lastExceptionDate.isAfter(
                referenceDate
            )
        ) {
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
                 * 정기 일정을 절대 같이 사용하지 않습니다.
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
                        schedules,
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
     * 특정 날짜 ScheduleException으로부터
     * 다음 배출 후보를 만듭니다.
     */
    private DayScheduleCandidate
    buildExceptionCandidate(
        ScheduleException exception,
        LocalDate candidateDate,
        LocalDateTime referenceDateTime
    ) {
        if (exception.isUnavailable()) {
            return DayScheduleCandidate.unavailable();
        }

        if (
            Boolean.TRUE.equals(
                exception.getAlwaysAvailable()
            )
        ) {
            return DayScheduleCandidate.available(
                null
            );
        }

        if (!exception.hasTimeWindow()) {
            return DayScheduleCandidate.unavailable();
        }

        LocalTime startTime =
            exception.getStartTime();

        LocalTime endTime =
            exception.getEndTime();

        if (
            candidateDate.equals(
                referenceDateTime.toLocalDate()
            )
        ) {
            LocalTime currentTime =
                referenceDateTime.toLocalTime();

            /*
             * 일반 시간 범위에서 종료 시각이 지났다면
             * 오늘 예외 일정은 이미 끝났습니다.
             */
            if (
                !exception.isOvernight()
                    && !currentTime.isBefore(
                    endTime
                )
            ) {
                return DayScheduleCandidate.unavailable();
            }
        }

        return DayScheduleCandidate.available(
            candidateDate.atTime(
                startTime
            )
        );
    }

    /**
     * 특정 날짜에 적용되는
     * 기존 반복 일정 후보를 계산합니다.
     */
    private DayScheduleCandidate
    buildRegularCandidate(
        List<RecycleSchedule> schedules,
        LocalDate candidateDate,
        LocalDateTime referenceDateTime
    ) {
        /*
         * 상시 배출 일정이 하나라도 존재하면
         * 예외가 없는 모든 날짜에 배출 가능합니다.
         */
        boolean alwaysAvailable =
            schedules.stream()
                .anyMatch(
                    RecycleSchedule::isAlwaysAvailable
                );

        if (alwaysAvailable) {
            return DayScheduleCandidate.available(
                null
            );
        }

        DayOfWeek candidateDay =
            candidateDate.getDayOfWeek();

        List<RecycleSchedule> daySchedules =
            schedules.stream()
                .filter(
                    schedule ->
                        schedule.getDayOfWeek()
                            == candidateDay
                )
                .toList();

        if (daySchedules.isEmpty()) {
            return DayScheduleCandidate.unavailable();
        }

        LocalDateTime nearest =
            null;

        for (
            RecycleSchedule schedule
            : daySchedules
        ) {
            if (
                candidateDate.equals(
                    referenceDateTime.toLocalDate()
                )
                    && !referenceDateTime
                    .toLocalTime()
                    .isBefore(
                        schedule.getEndTime()
                    )
            ) {
                /*
                 * 오늘 일정이 이미 종료된 경우
                 * 이 일정은 다음 후보에서 제외합니다.
                 */
                continue;
            }

            LocalDateTime candidate =
                candidateDate.atTime(
                    schedule.getStartTime()
                );

            if (
                nearest == null
                    || candidate.isBefore(
                    nearest
                )
            ) {
                nearest = candidate;
            }
        }

        if (nearest == null) {
            return DayScheduleCandidate.unavailable();
        }

        return DayScheduleCandidate.available(
            nearest
        );
    }

    /**
     * 특정 날짜의 ScheduleException을 찾습니다.
     */
    private ScheduleException findException(
        List<ScheduleException> exceptions,
        LocalDate date
    ) {
        return exceptions.stream()
            .filter(
                exception ->
                    Objects.equals(
                        exception.getEffectiveDate(),
                        date
                    )
            )
            .findFirst()
            .orElse(
                null
            );
    }

    /**
     * 특정 품목의 예외 일정 조회
     *
     * 전날 overnight 예외까지 계산하기 위해
     * 기준 날짜 하루 전부터 조회합니다.
     */
    private List<ScheduleException>
    getWasteItemExceptions(
        Long apartmentId,
        Long wasteItemId,
        LocalDate referenceDate
    ) {
        return scheduleExceptionRepository
            .findAllByApartmentIdAndWasteItemIdAndEffectiveDateGreaterThanEqualOrderByEffectiveDateAsc(
                apartmentId,
                wasteItemId,
                referenceDate.minusDays(1)
            );
    }

    /**
     * 로그인 사용자 조회
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
     * 사용자가 설정한 승인된 거주 아파트 조회
     */
    private Apartment getUserApartment(
        User user
    ) {
        Apartment apartment =
            user.getApartment();

        if (apartment == null) {
            throw new CustomException(
                ErrorCode.USER_APARTMENT_NOT_SET
            );
        }

        if (
            apartment.getStatus()
                != ApartmentStatus.APPROVED
        ) {
            throw new CustomException(
                ErrorCode.APARTMENT_NOT_APPROVED
            );
        }

        return apartment;
    }

    /**
     * 승인된 아파트 조회
     */
    private Apartment getApprovedApartment(
        Long apartmentId
    ) {
        Apartment apartment =
            apartmentRepository
                .findById(
                    apartmentId
                )
                .orElseThrow(
                    () ->
                        new CustomException(
                            ErrorCode.APARTMENT_NOT_FOUND
                        )
                );

        if (
            apartment.getStatus()
                != ApartmentStatus.APPROVED
        ) {
            throw new CustomException(
                ErrorCode.APARTMENT_NOT_APPROVED
            );
        }

        return apartment;
    }

    /**
     * 일반 사용자에게 노출 가능한
     * 활성 폐기물 품목 조회
     */
    private WasteItem getActiveWasteItem(
        Long wasteItemId
    ) {
        return wasteItemRepository
            .findByIdAndActiveTrue(
                wasteItemId
            )
            .orElseThrow(
                () ->
                    new CustomException(
                        ErrorCode.WASTE_ITEM_NOT_FOUND
                    )
            );
    }

    /**
     * 특정 날짜의 배출 후보 계산 결과입니다.
     */
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

    /**
     * 최종 다음 배출 일정 계산 결과입니다.
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