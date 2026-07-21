package com.smartrecycle.backend.domain.schedule.service;

import com.smartrecycle.backend.domain.apartment.entity.Apartment;
import com.smartrecycle.backend.domain.apartment.entity.ApartmentStatus;
import com.smartrecycle.backend.domain.apartment.repository.ApartmentRepository;
import com.smartrecycle.backend.domain.schedule.dto.response.ApartmentScheduleResponse;
import com.smartrecycle.backend.domain.schedule.dto.response.RecycleScheduleResponse;
import com.smartrecycle.backend.domain.schedule.dto.response.RecycleScheduleTimeResponse;
import com.smartrecycle.backend.domain.schedule.dto.response.WasteItemScheduleResponse;
import com.smartrecycle.backend.domain.schedule.entity.RecycleSchedule;
import com.smartrecycle.backend.domain.schedule.repository.RecycleScheduleRepository;
import com.smartrecycle.backend.domain.user.entity.User;
import com.smartrecycle.backend.domain.user.repository.UserRepository;
import com.smartrecycle.backend.domain.waste.entity.WasteItem;
import com.smartrecycle.backend.domain.waste.repository.WasteItemRepository;
import com.smartrecycle.backend.global.exception.CustomException;
import com.smartrecycle.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecycleScheduleQueryService {

    private final RecycleScheduleRepository
            recycleScheduleRepository;

    private final ApartmentRepository apartmentRepository;
    private final WasteItemRepository wasteItemRepository;
    private final UserRepository userRepository;

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
                        .findDetailById(scheduleId)
                        .orElseThrow(
                                () -> new CustomException(
                                        ErrorCode
                                                .RECYCLE_SCHEDULE_NOT_FOUND
                                )
                        );

        return RecycleScheduleResponse.from(
                schedule
        );
    }

    /**
     * 특정 아파트에 등록된 공식 배출 일정 전체를 조회합니다.
     *
     * 관리자 일정 목록 화면에서 사용합니다.
     */
    public List<RecycleScheduleResponse>
    getSchedulesByApartment(
            Long apartmentId
    ) {
        getApprovedApartment(apartmentId);

        return recycleScheduleRepository
                .findAllByApartmentId(apartmentId)
                .stream()
                .map(RecycleScheduleResponse::from)
                .toList();
    }

    /**
     * 로그인한 사용자의 거주 아파트에 등록된
     * 전체 공식 배출 일정을 품목별로 계산해 반환합니다.
     */
    public ApartmentScheduleResponse
    getMyApartmentSchedule(
            Long userId
    ) {
        User user = getUser(userId);
        Apartment apartment =
                getUserApartment(user);

        LocalDateTime referenceDateTime =
                LocalDateTime.now();

        return buildApartmentScheduleResponse(
                apartment,
                referenceDateTime
        );
    }

    /**
     * 로그인한 사용자의 거주 아파트를 기준으로
     * 특정 폐기물 품목의 공식 일정을 계산합니다.
     */
    public WasteItemScheduleResponse
    getMyWasteItemSchedule(
            Long userId,
            Long wasteItemId
    ) {
        User user = getUser(userId);
        Apartment apartment =
                getUserApartment(user);

        WasteItem wasteItem =
                getActiveWasteItem(wasteItemId);

        List<RecycleSchedule> schedules =
                recycleScheduleRepository
                        .findAllByApartmentIdAndWasteItemId(
                                apartment.getId(),
                                wasteItem.getId()
                        );

        return buildWasteItemScheduleResponse(
                wasteItem,
                schedules,
                LocalDateTime.now()
        );
    }

    /**
     * 지정된 아파트와 폐기물 품목을 기준으로
     * 공식 일정을 계산합니다.
     *
     * 폐기물 가이드 응답에 일정 정보를 결합할 때
     * 내부 서비스에서 재사용할 수 있습니다.
     */
    public WasteItemScheduleResponse
    getWasteItemSchedule(
            Long apartmentId,
            Long wasteItemId
    ) {
        Apartment apartment =
                getApprovedApartment(apartmentId);

        WasteItem wasteItem =
                getActiveWasteItem(wasteItemId);

        List<RecycleSchedule> schedules =
                recycleScheduleRepository
                        .findAllByApartmentIdAndWasteItemId(
                                apartment.getId(),
                                wasteItem.getId()
                        );

        return buildWasteItemScheduleResponse(
                wasteItem,
                schedules,
                LocalDateTime.now()
        );
    }

    /**
     * 아파트의 전체 일정을 품목별로 묶어 계산합니다.
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
         * 같은 폐기물 품목에 월요일, 수요일 등
         * 여러 일정이 존재할 수 있으므로 품목 ID별로 묶습니다.
         *
         * LinkedHashMap을 사용하여 Repository에서 조회된
         * 품목 정렬 순서를 유지합니다.
         */
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

        List<WasteItemScheduleResponse> items =
                schedulesByWasteItem.values()
                        .stream()
                        .map(itemSchedules -> {
                            WasteItem wasteItem =
                                    itemSchedules
                                            .get(0)
                                            .getWasteItem();

                            return buildWasteItemScheduleResponse(
                                    wasteItem,
                                    itemSchedules,
                                    referenceDateTime
                            );
                        })
                        .toList();

        return ApartmentScheduleResponse.of(
                apartment,
                referenceDateTime,
                items
        );
    }

    /**
     * 한 품목의 일정 목록을 바탕으로
     * 오늘 배출 가능 여부와 다음 배출일을 계산합니다.
     */
    private WasteItemScheduleResponse
    buildWasteItemScheduleResponse(
            WasteItem wasteItem,
            List<RecycleSchedule> schedules,
            LocalDateTime referenceDateTime
    ) {
        boolean availableToday =
                isAvailableToday(
                        schedules,
                        referenceDateTime.toLocalDate()
                );

        boolean availableNow =
                isAvailableNow(
                        schedules,
                        referenceDateTime
                );

        NextScheduleResult nextSchedule =
                calculateNextSchedule(
                        schedules,
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
                scheduleResponses
        );
    }

    /**
     * 오늘 배출 일정이 존재하는지 확인합니다.
     *
     * 현재 배출 시간이 이미 지났더라도
     * 오늘 일정 자체가 존재하면 true입니다.
     */
    private boolean isAvailableToday(
            List<RecycleSchedule> schedules,
            LocalDate referenceDate
    ) {
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
     * 현재 시각에 실제로 배출할 수 있는지 확인합니다.
     */
    private boolean isAvailableNow(
            List<RecycleSchedule> schedules,
            LocalDateTime referenceDateTime
    ) {
        DayOfWeek today =
                referenceDateTime
                        .getDayOfWeek();

        LocalTime currentTime =
                referenceDateTime
                        .toLocalTime();

        return schedules.stream()
                .anyMatch(schedule -> {
                    if (schedule.isAlwaysAvailable()) {
                        return true;
                    }

                    if (schedule.getDayOfWeek() != today) {
                        return false;
                    }

                    LocalTime startTime =
                            schedule.getStartTime();

                    LocalTime endTime =
                            schedule.getEndTime();

                    /*
                     * 시작 시간은 포함하고 종료 시간은 포함하지 않습니다.
                     *
                     * 예:
                     * 18:00~22:00 일정이라면
                     * 18:00은 가능, 22:00은 불가능합니다.
                     */
                    boolean started =
                            !currentTime.isBefore(
                                    startTime
                            );

                    boolean notEnded =
                            currentTime.isBefore(
                                    endTime
                            );

                    return started && notEnded;
                });
    }

    /**
     * 현재 시각을 기준으로 가장 가까운 배출 일정을 계산합니다.
     */
    private NextScheduleResult
    calculateNextSchedule(
            List<RecycleSchedule> schedules,
            LocalDateTime referenceDateTime
    ) {
        if (schedules.isEmpty()) {
            return NextScheduleResult.empty();
        }

        boolean alwaysAvailable =
                schedules.stream()
                        .anyMatch(
                                RecycleSchedule
                                        ::isAlwaysAvailable
                        );

        if (alwaysAvailable) {
            return new NextScheduleResult(
                    referenceDateTime.toLocalDate(),
                    null
            );
        }

        LocalDateTime nearestSchedule = null;

        for (RecycleSchedule schedule : schedules) {
            LocalDateTime candidate =
                    calculateCandidateDateTime(
                            schedule,
                            referenceDateTime
                    );

            if (
                    nearestSchedule == null
                            || candidate.isBefore(
                            nearestSchedule
                    )
            ) {
                nearestSchedule = candidate;
            }
        }

        if (nearestSchedule == null) {
            return NextScheduleResult.empty();
        }

        return new NextScheduleResult(
                nearestSchedule.toLocalDate(),
                nearestSchedule
        );
    }

    /**
     * 주간 반복 일정 하나의 가장 가까운 날짜와 시간을 계산합니다.
     */
    private LocalDateTime
    calculateCandidateDateTime(
            RecycleSchedule schedule,
            LocalDateTime referenceDateTime
    ) {
        LocalDate referenceDate =
                referenceDateTime.toLocalDate();

        LocalTime referenceTime =
                referenceDateTime.toLocalTime();

        DayOfWeek currentDay =
                referenceDate.getDayOfWeek();

        DayOfWeek scheduleDay =
                schedule.getDayOfWeek();

        int daysUntilSchedule =
                (
                        scheduleDay.getValue()
                                - currentDay.getValue()
                                + 7
                ) % 7;

        LocalDate candidateDate =
                referenceDate.plusDays(
                        daysUntilSchedule
                );

        /*
         * 오늘 일정이지만 종료 시간이 지났다면
         * 다음 주 같은 요일로 이동합니다.
         */
        if (
                daysUntilSchedule == 0
                        && !referenceTime.isBefore(
                        schedule.getEndTime()
                )
        ) {
            candidateDate =
                    candidateDate.plusDays(7);
        }

        return candidateDate.atTime(
                schedule.getStartTime()
        );
    }

    /**
     * 로그인 사용자를 조회합니다.
     */
    private User getUser(
            Long userId
    ) {
        return userRepository
                .findById(userId)
                .orElseThrow(
                        () -> new CustomException(
                                ErrorCode.USER_NOT_FOUND
                        )
                );
    }

    /**
     * 사용자가 설정한 거주 아파트를 조회합니다.
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
     * 승인된 아파트를 조회합니다.
     */
    private Apartment getApprovedApartment(
            Long apartmentId
    ) {
        Apartment apartment =
                apartmentRepository
                        .findById(apartmentId)
                        .orElseThrow(
                                () -> new CustomException(
                                        ErrorCode
                                                .APARTMENT_NOT_FOUND
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
     * 일반 사용자에게 노출 가능한 활성 품목을 조회합니다.
     */
    private WasteItem getActiveWasteItem(
            Long wasteItemId
    ) {
        return wasteItemRepository
                .findByIdAndActiveTrue(
                        wasteItemId
                )
                .orElseThrow(
                        () -> new CustomException(
                                ErrorCode
                                        .WASTE_ITEM_NOT_FOUND
                        )
                );
    }

    /**
     * 다음 배출 일정 계산 결과를
     * 내부에서 전달하기 위한 값 객체입니다.
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