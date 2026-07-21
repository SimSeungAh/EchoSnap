package com.smartrecycle.backend.domain.waste.service;

import com.smartrecycle.backend.domain.schedule.dto.response.WasteItemScheduleResponse;
import com.smartrecycle.backend.domain.schedule.service.RecycleScheduleQueryService;
import com.smartrecycle.backend.domain.waste.dto.response.WasteCategoryResponse;
import com.smartrecycle.backend.domain.waste.dto.response.WasteItemDetailResponse;
import com.smartrecycle.backend.domain.waste.dto.response.WasteItemSummaryResponse;
import com.smartrecycle.backend.domain.waste.entity.RecycleGuide;
import com.smartrecycle.backend.domain.waste.entity.RecycleGuideCheckItem;
import com.smartrecycle.backend.domain.waste.entity.WasteCategory;
import com.smartrecycle.backend.domain.waste.entity.WasteItem;
import com.smartrecycle.backend.domain.waste.repository.RecycleGuideCheckItemRepository;
import com.smartrecycle.backend.domain.waste.repository.RecycleGuideRepository;
import com.smartrecycle.backend.domain.waste.repository.WasteCategoryRepository;
import com.smartrecycle.backend.domain.waste.repository.WasteItemRepository;
import com.smartrecycle.backend.global.exception.CustomException;
import com.smartrecycle.backend.global.exception.ErrorCode;
import com.smartrecycle.backend.global.response.PageResponse;
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
     * 반환 정보:
     * - 품목 기본 정보
     * - 소속 카테고리
     * - 분리배출 가이드
     * - 체크리스트
     * - 로그인 사용자의 아파트 배출 일정
     * - 오늘 배출 가능 여부
     * - 현재 배출 가능 여부
     * - 다음 배출일
     *
     * 품목에 가이드가 아직 등록되지 않은 경우
     * guide는 null로 반환합니다.
     */
    public WasteItemDetailResponse getItemDetail(
            Long userId,
            Long wasteItemId
    ) {
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

        /*
         * 로그인 사용자가 설정한 거주 아파트를 기준으로
         * 해당 품목의 공식 배출 일정을 계산합니다.
         *
         * 사용자가 아직 아파트를 설정하지 않았다면
         * USER_APARTMENT_NOT_SET 오류가 발생합니다.
         */
        WasteItemScheduleResponse schedule =
                recycleScheduleQueryService
                        .getMyWasteItemSchedule(
                                userId,
                                wasteItemId
                        );

        return WasteItemDetailResponse.from(
                wasteItem,
                recycleGuide,
                checkItems,
                schedule
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
        if (keyword == null || keyword.isBlank()) {
            return "";
        }

        return keyword.trim();
    }
}