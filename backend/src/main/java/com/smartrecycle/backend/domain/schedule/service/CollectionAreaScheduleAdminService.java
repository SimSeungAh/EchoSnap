package com.smartrecycle.backend.domain.schedule.service;

import com.smartrecycle.backend.domain.collectionarea.entity.CollectionArea;
import com.smartrecycle.backend.domain.collectionarea.entity.CollectionAreaSourceType;
import com.smartrecycle.backend.domain.collectionarea.entity.CollectionWasteType;
import com.smartrecycle.backend.domain.collectionarea.repository.CollectionAreaRepository;
import com.smartrecycle.backend.domain.schedule.dto.admin.AdminCollectionAreaScheduleDtos;
import com.smartrecycle.backend.domain.schedule.entity.CollectionAreaSchedule;
import com.smartrecycle.backend.domain.schedule.entity.CollectionAreaScheduleSourceType;
import com.smartrecycle.backend.domain.schedule.repository.CollectionAreaScheduleRepository;
import com.smartrecycle.backend.domain.user.entity.Role;
import com.smartrecycle.backend.domain.user.entity.User;
import com.smartrecycle.backend.domain.user.entity.UserStatus;
import com.smartrecycle.backend.domain.user.repository.UserRepository;
import com.smartrecycle.backend.global.exception.CustomException;
import com.smartrecycle.backend.global.exception.ErrorCode;
import com.smartrecycle.backend.global.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CollectionAreaScheduleAdminService {

    private final UserRepository userRepository;

    private final CollectionAreaRepository
        collectionAreaRepository;

    private final CollectionAreaScheduleRepository
        collectionAreaScheduleRepository;

    public PageResponse<
        AdminCollectionAreaScheduleDtos.ScheduleResponse
        >
    search(
        Long adminId,
        String keyword,
        Long collectionAreaId,
        CollectionWasteType wasteType,
        CollectionAreaScheduleSourceType sourceType,
        Pageable pageable
    ) {
        validateAdmin(adminId);

        Page<CollectionAreaSchedule> page =
            collectionAreaScheduleRepository
                .searchAdminSchedules(
                    normalizeKeyword(keyword),
                    collectionAreaId,
                    wasteType,
                    sourceType,
                    pageable
                );

        return PageResponse.from(
            page,
            AdminCollectionAreaScheduleDtos
                .ScheduleResponse
                ::from
        );
    }

    /**
     * 관리자 일반주택 배출일정 화면.
     *
     * 기존:
     *
     * CollectionArea 원본 20개 조회
     * → 그 20개 안에서 프론트 그룹화
     *
     * 수정:
     *
     * 실제 표시 지역 전체를 먼저 그룹화
     * → 지역 그룹 20개 페이지네이션
     * → 각 지역 그룹 안의 원본 일정은 상세 데이터로 제공
     */
    public PageResponse<
        AdminCollectionAreaScheduleDtos
            .AreaScheduleGroupResponse
        >
    searchCoverage(
        Long adminId,
        String keyword,
        CollectionAreaSourceType areaSourceType,
        Boolean active,
        Pageable pageable
    ) {
        validateAdmin(adminId);

        String normalizedKeyword =
            normalizeKeyword(keyword);

        /*
         * 1.
         * DB에서 먼저 실제 표시 지역 그룹을 만듭니다.
         *
         * 여기에는 CollectionArea 원본 8천여 개가 아니라
         * "정선군 북평면" 같은 지역 그룹이 들어옵니다.
         */
        List<
            CollectionAreaRepository
                .AdminAreaGroupProjection
            > allGroups =
            collectionAreaRepository
                .searchAdminAreaGroups(
                    normalizedKeyword,
                    areaSourceType,
                    active
                );

        int totalGroupCount =
            allGroups.size();

        int fromIndex =
            (int) Math.min(
                pageable.getOffset(),
                totalGroupCount
            );

        int toIndex =
            Math.min(
                fromIndex
                    + pageable.getPageSize(),
                totalGroupCount
            );

        /*
         * 2.
         * 현재 페이지에 표시할 "지역 그룹 20개"만 자릅니다.
         */
        List<
            CollectionAreaRepository
                .AdminAreaGroupProjection
            > pageGroups =
            allGroups.subList(
                fromIndex,
                toIndex
            );

        /*
         * 3.
         * 각 지역 그룹 내부의 실제 CollectionArea들을 로딩합니다.
         *
         * 서로 다른 배출방법/장소를 가진 원본은
         * 삭제하거나 합치지 않고 상세 정보로 보존합니다.
         */
        List<LoadedAreaGroup> loadedGroups =
            new ArrayList<>();

        List<Long> allAreaIds =
            new ArrayList<>();

        for (
            CollectionAreaRepository
                .AdminAreaGroupProjection group
            : pageGroups
        ) {
            boolean groupActive =
                Boolean.TRUE.equals(
                    group.getActive()
                );

            List<CollectionArea> areas =
                collectionAreaRepository
                    .findAllByAdminAreaGroup(
                        group.getSido(),
                        group.getSigungu(),
                        group.getAreaName(),
                        group.getTargetAreaName(),
                        group.getSourceType(),
                        groupActive
                    );

            loadedGroups.add(
                new LoadedAreaGroup(
                    areas
                )
            );

            allAreaIds.addAll(
                areas
                    .stream()
                    .map(
                        CollectionArea::getId
                    )
                    .toList()
            );
        }

        /*
         * 4.
         * 현재 페이지에 필요한 일정은 한 번에 조회합니다.
         */
        Map<Long, List<CollectionAreaSchedule>>
            schedulesByAreaId;

        if (allAreaIds.isEmpty()) {
            schedulesByAreaId =
                Map.of();
        } else {
            schedulesByAreaId =
                collectionAreaScheduleRepository
                    .findAllByCollectionAreaIdIn(
                        allAreaIds
                    )
                    .stream()
                    .collect(
                        Collectors.groupingBy(
                            schedule ->
                                schedule
                                    .getCollectionArea()
                                    .getId()
                        )
                    );
        }

        /*
         * 5.
         * 화면용 지역 그룹 DTO 생성.
         */
        List<
            AdminCollectionAreaScheduleDtos
                .AreaScheduleGroupResponse
            > groupResponses =
            loadedGroups
                .stream()
                .filter(
                    group ->
                        !group.areas()
                            .isEmpty()
                )
                .map(
                    group ->
                        AdminCollectionAreaScheduleDtos
                            .AreaScheduleGroupResponse
                            .from(
                                group.areas(),
                                schedulesByAreaId
                            )
                )
                .toList();

        /*
         * PageImpl의 totalElements는
         * CollectionArea 원본 수가 아니라
         * 실제 표시 지역 그룹 수입니다.
         */
        Page<
            AdminCollectionAreaScheduleDtos
                .AreaScheduleGroupResponse
            > groupPage =
            new PageImpl<>(
                groupResponses,
                pageable,
                totalGroupCount
            );

        return PageResponse.from(
            groupPage,
            response -> response
        );
    }

    public AdminCollectionAreaScheduleDtos.ScheduleResponse
    get(
        Long adminId,
        Long scheduleId
    ) {
        validateAdmin(adminId);

        return AdminCollectionAreaScheduleDtos
            .ScheduleResponse
            .from(
                getSchedule(
                    scheduleId
                )
            );
    }

    @Transactional
    public AdminCollectionAreaScheduleDtos.ScheduleResponse
    create(
        Long adminId,
        AdminCollectionAreaScheduleDtos.CreateRequest request
    ) {
        validateAdmin(adminId);

        validateTimePair(
            request.startTime(),
            request.endTime()
        );

        CollectionArea area =
            collectionAreaRepository
                .findById(
                    request.collectionAreaId()
                )
                .orElseThrow(
                    () ->
                        new CustomException(
                            ErrorCode.INVALID_INPUT
                        )
                );

        if (
            !area.getSupportedWasteTypes()
                .contains(
                    request.wasteType()
                )
        ) {
            throw new CustomException(
                ErrorCode.INVALID_RECYCLE_SCHEDULE
            );
        }

        boolean alreadyExists =
            collectionAreaScheduleRepository
                .findByCollectionAreaIdAndWasteType(
                    area.getId(),
                    request.wasteType()
                )
                .isPresent();

        if (alreadyExists) {
            throw new CustomException(
                ErrorCode.RECYCLE_SCHEDULE_ALREADY_EXISTS
            );
        }

        CollectionAreaSchedule schedule =
            CollectionAreaSchedule
                .createFromApprovedReport(
                    area,
                    request.wasteType(),
                    request.emissionDays()
                        .trim(),
                    request.startTime(),
                    request.endTime()
                );

        CollectionAreaSchedule saved =
            collectionAreaScheduleRepository
                .save(
                    schedule
                );

        return AdminCollectionAreaScheduleDtos
            .ScheduleResponse
            .from(
                saved
            );
    }

    @Transactional
    public AdminCollectionAreaScheduleDtos.ScheduleResponse
    update(
        Long adminId,
        Long scheduleId,
        AdminCollectionAreaScheduleDtos.UpdateRequest request
    ) {
        validateAdmin(adminId);

        validateTimePair(
            request.startTime(),
            request.endTime()
        );

        CollectionAreaSchedule schedule =
            getSchedule(
                scheduleId
            );

        schedule.updateFromApprovedReport(
            request.emissionDays()
                .trim(),
            request.startTime(),
            request.endTime()
        );

        return AdminCollectionAreaScheduleDtos
            .ScheduleResponse
            .from(
                schedule
            );
    }

    @Transactional
    public void delete(
        Long adminId,
        Long scheduleId
    ) {
        validateAdmin(adminId);

        CollectionAreaSchedule schedule =
            getSchedule(
                scheduleId
            );

        collectionAreaScheduleRepository
            .delete(
                schedule
            );
    }

    private CollectionAreaSchedule getSchedule(
        Long scheduleId
    ) {
        return collectionAreaScheduleRepository
            .findById(
                scheduleId
            )
            .orElseThrow(
                () ->
                    new CustomException(
                        ErrorCode.RECYCLE_SCHEDULE_NOT_FOUND
                    )
            );
    }

    private void validateTimePair(
        LocalTime startTime,
        LocalTime endTime
    ) {
        boolean startExists =
            startTime != null;

        boolean endExists =
            endTime != null;

        if (startExists != endExists) {
            throw new CustomException(
                ErrorCode.INVALID_RECYCLE_SCHEDULE
            );
        }

        if (
            startExists
                && startTime.equals(
                endTime
            )
        ) {
            throw new CustomException(
                ErrorCode.INVALID_RECYCLE_SCHEDULE
            );
        }
    }

    private void validateAdmin(
        Long adminId
    ) {
        User admin =
            userRepository
                .findById(
                    adminId
                )
                .orElseThrow(
                    () ->
                        new CustomException(
                            ErrorCode.USER_NOT_FOUND
                        )
                );

        if (
            admin.getRole()
                != Role.ADMIN
                || admin.getStatus()
                != UserStatus.ACTIVE
        ) {
            throw new CustomException(
                ErrorCode.FORBIDDEN
            );
        }
    }

    private String normalizeKeyword(
        String keyword
    ) {
        if (keyword == null) {
            return "";
        }

        return keyword.trim();
    }

    /**
     * Service 내부에서만 사용하는
     * 페이지 지역 그룹 로딩 결과.
     */
    private record LoadedAreaGroup(
        List<CollectionArea> areas
    ) {
    }
}