package com.echosnap.backend.domain.schedule.service;

import com.echosnap.backend.domain.apartment.entity.Apartment;
import com.echosnap.backend.domain.apartment.entity.ApartmentStatus;
import com.echosnap.backend.domain.apartment.repository.ApartmentRepository;
import com.echosnap.backend.domain.schedule.dto.request.CreateRecycleScheduleRequest;
import com.echosnap.backend.domain.schedule.dto.request.UpdateRecycleScheduleRequest;
import com.echosnap.backend.domain.schedule.dto.response.RecycleScheduleResponse;
import com.echosnap.backend.domain.schedule.entity.RecycleSchedule;
import com.echosnap.backend.domain.schedule.repository.RecycleScheduleRepository;
import com.echosnap.backend.domain.user.entity.Role;
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
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecycleScheduleAdminService {

    private final RecycleScheduleRepository
            recycleScheduleRepository;

    private final ApartmentRepository apartmentRepository;
    private final WasteItemRepository wasteItemRepository;
    private final UserRepository userRepository;

    /**
     * 관리자가 공식 배출 일정을 등록합니다.
     */
    @Transactional
    public RecycleScheduleResponse createSchedule(
            Long adminId,
            CreateRecycleScheduleRequest request
    ) {
        getAdmin(adminId);

        Apartment apartment = getApprovedApartment(
                request.apartmentId()
        );

        WasteItem wasteItem = getWasteItem(
                request.wasteItemId()
        );

        validateScheduleInformation(
                request.alwaysAvailable(),
                request.dayOfWeek(),
                request.startTime(),
                request.endTime()
        );

        validateDuplicateForCreate(
                apartment.getId(),
                wasteItem.getId(),
                request.alwaysAvailable(),
                request.dayOfWeek()
        );

        RecycleSchedule schedule;

        if (Boolean.TRUE.equals(
                request.alwaysAvailable()
        )) {
            schedule =
                    RecycleSchedule
                            .createAlwaysAvailable(
                                    apartment,
                                    wasteItem
                            );
        } else {
            schedule =
                    RecycleSchedule.createWeekly(
                            apartment,
                            wasteItem,
                            request.dayOfWeek(),
                            request.startTime(),
                            request.endTime()
                    );
        }

        RecycleSchedule savedSchedule =
                recycleScheduleRepository.save(
                        schedule
                );

        return RecycleScheduleResponse.from(
                savedSchedule
        );
    }

    /**
     * 관리자가 기존 공식 배출 일정의
     * 요일, 시간 또는 상시 배출 여부를 수정합니다.
     *
     * 아파트와 품목은 변경하지 않습니다.
     */
    @Transactional
    public RecycleScheduleResponse updateSchedule(
            Long adminId,
            Long scheduleId,
            UpdateRecycleScheduleRequest request
    ) {
        getAdmin(adminId);

        RecycleSchedule schedule =
                getSchedule(scheduleId);

        validateScheduleInformation(
                request.alwaysAvailable(),
                request.dayOfWeek(),
                request.startTime(),
                request.endTime()
        );

        validateDuplicateForUpdate(
                schedule,
                request.alwaysAvailable(),
                request.dayOfWeek()
        );

        if (Boolean.TRUE.equals(
                request.alwaysAvailable()
        )) {
            schedule.updateAlwaysAvailable();
        } else {
            schedule.updateWeekly(
                    request.dayOfWeek(),
                    request.startTime(),
                    request.endTime()
            );
        }

        return RecycleScheduleResponse.from(
                schedule
        );
    }

    /**
     * 관리자가 공식 배출 일정을 삭제합니다.
     */
    @Transactional
    public void deleteSchedule(
            Long adminId,
            Long scheduleId
    ) {
        getAdmin(adminId);

        RecycleSchedule schedule =
                getSchedule(scheduleId);

        recycleScheduleRepository.delete(
                schedule
        );
    }

    /**
     * 관리자 ID로 사용자를 조회하고
     * 실제 ADMIN 권한인지 확인합니다.
     */
    private User getAdmin(
            Long adminId
    ) {
        User user = userRepository
                .findById(adminId)
                .orElseThrow(
                        () -> new CustomException(
                                ErrorCode.USER_NOT_FOUND
                        )
                );

        if (user.getRole() != Role.ADMIN) {
            throw new CustomException(
                    ErrorCode.FORBIDDEN
            );
        }

        return user;
    }

    /**
     * 공식 일정을 등록할 승인된 아파트를 조회합니다.
     */
    private Apartment getApprovedApartment(
            Long apartmentId
    ) {
        Apartment apartment = apartmentRepository
                .findById(apartmentId)
                .orElseThrow(
                        () -> new CustomException(
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
     * 일정에 연결할 폐기물 품목을 조회합니다.
     *
     * 관리자는 비활성 품목도 조회할 수 있으므로
     * active 조건 없이 ID로 조회합니다.
     */
    private WasteItem getWasteItem(
            Long wasteItemId
    ) {
        return wasteItemRepository
                .findById(wasteItemId)
                .orElseThrow(
                        () -> new CustomException(
                                ErrorCode.WASTE_ITEM_NOT_FOUND
                        )
                );
    }

    /**
     * 일정 상세 정보를 조회합니다.
     */
    private RecycleSchedule getSchedule(
            Long scheduleId
    ) {
        return recycleScheduleRepository
                .findDetailById(scheduleId)
                .orElseThrow(
                        () -> new CustomException(
                                ErrorCode
                                        .RECYCLE_SCHEDULE_NOT_FOUND
                        )
                );
    }

    /**
     * 상시 배출 여부에 따라
     * 요일과 시간 입력값 조합을 검사합니다.
     */
    private void validateScheduleInformation(
            Boolean alwaysAvailable,
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime
    ) {
        if (alwaysAvailable == null) {
            throw new CustomException(
                    ErrorCode.INVALID_RECYCLE_SCHEDULE
            );
        }

        if (alwaysAvailable) {
            validateAlwaysAvailableSchedule(
                    dayOfWeek,
                    startTime,
                    endTime
            );

            return;
        }

        validateWeeklySchedule(
                dayOfWeek,
                startTime,
                endTime
        );
    }

    /**
     * 상시 배출 일정은 요일과 시간을 입력할 수 없습니다.
     */
    private void validateAlwaysAvailableSchedule(
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime
    ) {
        boolean hasWeeklyInformation =
                dayOfWeek != null
                        || startTime != null
                        || endTime != null;

        if (hasWeeklyInformation) {
            throw new CustomException(
                    ErrorCode.INVALID_RECYCLE_SCHEDULE
            );
        }
    }

    /**
     * 일반 주간 일정은 요일, 시작 시간,
     * 종료 시간이 모두 필요합니다.
     */
    private void validateWeeklySchedule(
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime
    ) {
        boolean hasMissingInformation =
                dayOfWeek == null
                        || startTime == null
                        || endTime == null;

        if (hasMissingInformation) {
            throw new CustomException(
                    ErrorCode.INVALID_RECYCLE_SCHEDULE
            );
        }

        if (!startTime.isBefore(endTime)) {
            throw new CustomException(
                    ErrorCode
                            .INVALID_RECYCLE_SCHEDULE_TIME
            );
        }
    }

    /**
     * 신규 일정 등록 시 중복 여부를 검사합니다.
     *
     * 상시 배출:
     * 같은 아파트와 품목에 다른 일정이 하나라도 있으면 등록 불가
     *
     * 요일 배출:
     * 상시 일정 또는 같은 요일 일정이 있으면 등록 불가
     */
    private void validateDuplicateForCreate(
            Long apartmentId,
            Long wasteItemId,
            Boolean alwaysAvailable,
            DayOfWeek dayOfWeek
    ) {
        if (Boolean.TRUE.equals(alwaysAvailable)) {
            boolean scheduleExists =
                    recycleScheduleRepository
                            .existsByApartmentIdAndWasteItemId(
                                    apartmentId,
                                    wasteItemId
                            );

            if (scheduleExists) {
                throw new CustomException(
                        ErrorCode
                                .RECYCLE_SCHEDULE_ALREADY_EXISTS
                );
            }

            return;
        }

        boolean alwaysScheduleExists =
                recycleScheduleRepository
                        .existsByApartmentIdAndWasteItemIdAndAlwaysAvailableTrue(
                                apartmentId,
                                wasteItemId
                        );

        boolean sameDayScheduleExists =
                recycleScheduleRepository
                        .existsByApartmentIdAndWasteItemIdAndDayOfWeek(
                                apartmentId,
                                wasteItemId,
                                dayOfWeek
                        );

        if (
                alwaysScheduleExists
                        || sameDayScheduleExists
        ) {
            throw new CustomException(
                    ErrorCode
                            .RECYCLE_SCHEDULE_ALREADY_EXISTS
            );
        }
    }

    /**
     * 일정 수정 시 현재 일정을 제외하고
     * 중복 여부를 검사합니다.
     */
    private void validateDuplicateForUpdate(
            RecycleSchedule schedule,
            Boolean alwaysAvailable,
            DayOfWeek dayOfWeek
    ) {
        Long scheduleId = schedule.getId();

        Long apartmentId =
                schedule.getApartment().getId();

        Long wasteItemId =
                schedule.getWasteItem().getId();

        if (Boolean.TRUE.equals(alwaysAvailable)) {
            boolean anotherScheduleExists =
                    recycleScheduleRepository
                            .existsByApartmentIdAndWasteItemIdAndIdNot(
                                    apartmentId,
                                    wasteItemId,
                                    scheduleId
                            );

            if (anotherScheduleExists) {
                throw new CustomException(
                        ErrorCode
                                .RECYCLE_SCHEDULE_ALREADY_EXISTS
                );
            }

            return;
        }

        boolean anotherAlwaysScheduleExists =
                recycleScheduleRepository
                        .existsByApartmentIdAndWasteItemIdAndAlwaysAvailableTrueAndIdNot(
                                apartmentId,
                                wasteItemId,
                                scheduleId
                        );

        boolean anotherSameDayScheduleExists =
                recycleScheduleRepository
                        .existsByApartmentIdAndWasteItemIdAndDayOfWeekAndIdNot(
                                apartmentId,
                                wasteItemId,
                                dayOfWeek,
                                scheduleId
                        );

        if (
                anotherAlwaysScheduleExists
                        || anotherSameDayScheduleExists
        ) {
            throw new CustomException(
                    ErrorCode
                            .RECYCLE_SCHEDULE_ALREADY_EXISTS
            );
        }
    }
}