package com.echosnap.backend.domain.waste.service;

import com.echosnap.backend.domain.schedule.dto.response.WasteItemScheduleResponse;
import com.echosnap.backend.domain.schedule.service.RecycleScheduleQueryService;
import com.echosnap.backend.domain.user.entity.ResidenceType;
import com.echosnap.backend.domain.user.entity.User;
import com.echosnap.backend.domain.user.repository.UserRepository;
import com.echosnap.backend.domain.waste.dto.response.WasteCategoryResponse;
import com.echosnap.backend.domain.waste.dto.response.WasteItemDetailResponse;
import com.echosnap.backend.domain.waste.dto.response.WasteItemSummaryResponse;
import com.echosnap.backend.domain.waste.entity.RecycleGuide;
import com.echosnap.backend.domain.waste.entity.RecycleGuideCheckItem;
import com.echosnap.backend.domain.waste.entity.WasteCategory;
import com.echosnap.backend.domain.waste.entity.WasteItem;
import com.echosnap.backend.domain.waste.repository.RecycleGuideCheckItemRepository;
import com.echosnap.backend.domain.waste.repository.RecycleGuideRepository;
import com.echosnap.backend.domain.waste.repository.WasteCategoryRepository;
import com.echosnap.backend.domain.waste.repository.WasteItemRepository;
import com.echosnap.backend.global.exception.CustomException;
import com.echosnap.backend.global.exception.ErrorCode;
import com.echosnap.backend.global.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WasteQueryService {

    private final WasteCategoryRepository wasteCategoryRepository;
    private final WasteItemRepository wasteItemRepository;
    private final RecycleGuideRepository recycleGuideRepository;

    private final RecycleGuideCheckItemRepository
        recycleGuideCheckItemRepository;

    private final RecycleScheduleQueryService
        recycleScheduleQueryService;

    private final UserRepository userRepository;

    /**
     * 일반 사용자에게 노출할 활성 카테고리 목록을 조회합니다.
     *
     * sortOrder가 작은 카테고리부터 반환하고,
     * sortOrder가 같으면 이름순으로 정렬합니다.
     */
    public List<WasteCategoryResponse> getCategories() {
        return wasteCategoryRepository
            .findAllByActiveTrueOrderBySortOrderAscNameAsc()
            .stream()
            .map(WasteCategoryResponse::from)
            .toList();
    }

    /**
     * 일반 사용자용 폐기물 품목 목록을 검색합니다.
     *
     * keyword:
     * - 품목명과 추가 검색 키워드에서 검색
     * - null 또는 공백이면 전체 품목 조회
     *
     * categoryId:
     * - null이면 전체 카테고리 조회
     * - 값이 있으면 해당 활성 카테고리의 품목만 조회
     */
    public PageResponse<WasteItemSummaryResponse> searchItems(
        String keyword,
        Long categoryId,
        Pageable pageable
    ) {
        if (categoryId != null) {
            getActiveCategory(categoryId);
        }

        Page<WasteItem> wasteItemPage =
            wasteItemRepository.searchActiveItems(
                normalizeKeyword(keyword),
                categoryId,
                pageable
            );

        return PageResponse.from(
            wasteItemPage,
            WasteItemSummaryResponse::from
        );
    }

    /**
     * 일반 사용자용 폐기물 품목 상세 정보를 조회합니다.
     *
     * 공통 반환 정보:
     * - 품목 기본 정보
     * - 소속 카테고리
     * - 분리배출 가이드
     * - 체크리스트
     *
     * MANAGED_COMPLEX:
     * - 현재 선택한 공동주택의 품목별 일정도 함께 반환합니다.
     *
     * GENERAL_HOUSING:
     * - Apartment 일정을 강제로 조회하지 않습니다.
     * - 지역 수거구역 일정은
     *   /api/schedules/me/general-housing API에서 조회합니다.
     *
     * 품목에 가이드가 아직 등록되지 않은 경우
     * guide는 null로 반환합니다.
     */
    public WasteItemDetailResponse getItemDetail(
        Long userId,
        Long wasteItemId
    ) {
        User user = getUser(userId);

        WasteItem wasteItem =
            getActiveWasteItem(wasteItemId);

        /*
         * 품목 자체가 활성화되어 있어도
         * 상위 카테고리가 비활성화되어 있다면
         * 일반 사용자에게 노출하지 않습니다.
         */
        if (!wasteItem.getCategory().isActive()) {
            throw new CustomException(
                ErrorCode.WASTE_ITEM_NOT_FOUND
            );
        }

        RecycleGuide recycleGuide =
            recycleGuideRepository
                .findByWasteItemId(wasteItemId)
                .orElse(null);

        List<RecycleGuideCheckItem> checkItems =
            getCheckItems(recycleGuide);

        WasteItemScheduleResponse schedule = null;

        /*
         * 관리주체의 자체 일정을 사용하는
         * MANAGED_COMPLEX 사용자만 Apartment 기반
         * RecycleSchedule을 조회합니다.
         *
         * 일반주택 사용자는 별도의
         * CollectionAreaSchedule을 사용하므로
         * 여기서 Apartment 일정을 강제로 조회하지 않습니다.
         */
        if (
            user.getResidenceType()
                == ResidenceType.MANAGED_COMPLEX
        ) {
            schedule =
                recycleScheduleQueryService
                    .getMyWasteItemSchedule(
                        userId,
                        wasteItemId
                    );
        }

        return WasteItemDetailResponse.from(
            wasteItem,
            recycleGuide,
            checkItems,
            schedule
        );
    }

    /**
     * 로그인 사용자 조회
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
     * 활성화된 폐기물 카테고리를 조회합니다.
     */
    private WasteCategory getActiveCategory(
        Long categoryId
    ) {
        return wasteCategoryRepository
            .findByIdAndActiveTrue(categoryId)
            .orElseThrow(
                () -> new CustomException(
                    ErrorCode.WASTE_CATEGORY_NOT_FOUND
                )
            );
    }

    /**
     * 활성화된 폐기물 품목을 조회합니다.
     */
    private WasteItem getActiveWasteItem(
        Long wasteItemId
    ) {
        return wasteItemRepository
            .findByIdAndActiveTrue(wasteItemId)
            .orElseThrow(
                () -> new CustomException(
                    ErrorCode.WASTE_ITEM_NOT_FOUND
                )
            );
    }

    /**
     * 가이드가 존재하면 체크리스트를 표시 순서대로 조회하고,
     * 가이드가 없으면 빈 목록을 반환합니다.
     */
    private List<RecycleGuideCheckItem> getCheckItems(
        RecycleGuide recycleGuide
    ) {
        if (recycleGuide == null) {
            return List.of();
        }

        return recycleGuideCheckItemRepository
            .findAllByRecycleGuide_IdOrderBySortOrderAscIdAsc(
                recycleGuide.getId()
            );
    }

    /**
     * 검색어의 앞뒤 공백을 제거합니다.
     *
     * null 또는 공백만 입력된 경우
     * Repository 검색 조건에 맞춰 빈 문자열을 반환합니다.
     */
    private String normalizeKeyword(
        String keyword
    ) {
        if (
            keyword == null
                || keyword.isBlank()
        ) {
            return "";
        }

        return keyword.trim();
    }
}