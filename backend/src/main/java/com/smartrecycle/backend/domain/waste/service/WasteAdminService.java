package com.smartrecycle.backend.domain.waste.service;

import com.smartrecycle.backend.domain.user.entity.Role;
import com.smartrecycle.backend.domain.user.entity.User;
import com.smartrecycle.backend.domain.user.repository.UserRepository;
import com.smartrecycle.backend.domain.waste.dto.request.CreateWasteCategoryRequest;
import com.smartrecycle.backend.domain.waste.dto.request.CreateWasteItemRequest;
import com.smartrecycle.backend.domain.waste.dto.request.RecycleGuideCheckItemRequest;
import com.smartrecycle.backend.domain.waste.dto.request.SaveRecycleGuideRequest;
import com.smartrecycle.backend.domain.waste.dto.request.UpdateWasteCategoryRequest;
import com.smartrecycle.backend.domain.waste.dto.request.UpdateWasteItemRequest;
import com.smartrecycle.backend.domain.waste.dto.response.AdminWasteCategoryResponse;
import com.smartrecycle.backend.domain.waste.dto.response.AdminWasteItemResponse;
import com.smartrecycle.backend.domain.waste.dto.response.RecycleGuideResponse;
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
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WasteAdminService {

    private final WasteCategoryRepository wasteCategoryRepository;
    private final WasteItemRepository wasteItemRepository;
    private final RecycleGuideRepository recycleGuideRepository;
    private final RecycleGuideCheckItemRepository
            recycleGuideCheckItemRepository;
    private final UserRepository userRepository;

    /**
     * 관리자가 활성·비활성 상태를 포함한 전체 폐기물 카테고리를 조회
     */
    public List<AdminWasteCategoryResponse> getCategories(
            Long adminId
    ) {
        getAdmin(adminId);

        return wasteCategoryRepository
                .findAll(
                        Sort.by(
                                Sort.Order.asc("sortOrder"),
                                Sort.Order.asc("name")
                        )
                )
                .stream()
                .map(AdminWasteCategoryResponse::from)
                .toList();
    }

    /**
     * 관리자가 새로운 폐기물 카테고리를 등록
     */
    @Transactional
    public AdminWasteCategoryResponse createCategory(
            Long adminId,
            CreateWasteCategoryRequest request
    ) {
        getAdmin(adminId);

        String code = request.code().trim();

        validateDuplicateCategoryCode(code);

        WasteCategory category = WasteCategory.create(
                code,
                request.name().trim(),
                trimToNull(request.description()),
                request.sortOrder()
        );

        WasteCategory savedCategory =
                wasteCategoryRepository.save(category);

        return AdminWasteCategoryResponse.from(
                savedCategory
        );
    }

    /**
     * 관리자가 폐기물 카테고리 정보를 수정
     * UpdateWasteCategoryRequest의 active 값을 통해 비활성 카테고리를 다시 활성화할 수도 있음
     */
    @Transactional
    public AdminWasteCategoryResponse updateCategory(
            Long adminId,
            Long categoryId,
            UpdateWasteCategoryRequest request
    ) {
        getAdmin(adminId);

        WasteCategory category =
                getCategory(categoryId);

        String code = request.code().trim();

        validateDuplicateCategoryCodeForUpdate(
                categoryId,
                code
        );

        category.update(
                code,
                request.name().trim(),
                trimToNull(request.description()),
                request.sortOrder(),
                request.active()
        );

        return AdminWasteCategoryResponse.from(
                category
        );
    }

    /**
     * 관리자가 폐기물 카테고리를 비활성화
     * 카테고리는 실제 삭제하지 않음
     * 비활성 카테고리의 품목은 사용자 검색에 노출되지 않음
     */
    @Transactional
    public AdminWasteCategoryResponse deactivateCategory(
            Long adminId,
            Long categoryId
    ) {
        getAdmin(adminId);

        WasteCategory category =
                getCategory(categoryId);

        category.deactivate();

        return AdminWasteCategoryResponse.from(
                category
        );
    }

    /**
     * 관리자가 폐기물 품목 목록을 검색
     * active:
     * null  -> 전체
     * true  -> 활성 품목
     * false -> 비활성 품목
     */
    public PageResponse<AdminWasteItemResponse> searchItems(
            Long adminId,
            String keyword,
            Long categoryId,
            Boolean active,
            Pageable pageable
    ) {
        getAdmin(adminId);

        if (categoryId != null) {
            getCategory(categoryId);
        }

        Page<WasteItem> wasteItemPage =
                wasteItemRepository.searchAdminItems(
                        normalizeKeyword(keyword),
                        categoryId,
                        active,
                        pageable
                );

        return PageResponse.from(
                wasteItemPage,
                AdminWasteItemResponse::from
        );
    }

    /**
     * 관리자가 활성 상태와 관계없이 폐기물 품목 한 건을 조회
     */
    public AdminWasteItemResponse getItem(
            Long adminId,
            Long wasteItemId
    ) {
        getAdmin(adminId);

        WasteItem wasteItem =
                getWasteItem(wasteItemId);

        return AdminWasteItemResponse.from(
                wasteItem
        );
    }

    /**
     * 관리자가 새로운 폐기물 품목을 등록
     */
    @Transactional
    public AdminWasteItemResponse createItem(
            Long adminId,
            CreateWasteItemRequest request
    ) {
        getAdmin(adminId);

        WasteCategory category =
                getCategory(request.categoryId());

        String name = request.name().trim();

        validateDuplicateWasteItem(
                category.getId(),
                name
        );

        WasteItem wasteItem = WasteItem.create(
                category,
                name,
                trimToNull(request.searchKeywords()),
                trimToNull(request.imageUrl())
        );

        WasteItem savedWasteItem =
                wasteItemRepository.save(wasteItem);

        return AdminWasteItemResponse.from(
                savedWasteItem
        );
    }

    /**
     * 관리자가 폐기물 품목 정보를 수정
     * 다른 카테고리로 품목을 이동하는 것도 허용
     */
    @Transactional
    public AdminWasteItemResponse updateItem(
            Long adminId,
            Long wasteItemId,
            UpdateWasteItemRequest request
    ) {
        getAdmin(adminId);

        WasteItem wasteItem =
                getWasteItem(wasteItemId);

        WasteCategory category =
                getCategory(request.categoryId());

        String name = request.name().trim();

        validateDuplicateWasteItemForUpdate(
                wasteItemId,
                category.getId(),
                name
        );

        wasteItem.update(
                category,
                name,
                trimToNull(request.searchKeywords()),
                trimToNull(request.imageUrl())
        );

        return AdminWasteItemResponse.from(
                wasteItem
        );
    }

    /**
     * 관리자가 폐기물 품목을 비활성화
     * 기존 가이드와 이후 연결될 AI 분석 기록을 보호하기 위해 실제 DB 데이터는 삭제하지 않음
     */
    @Transactional
    public AdminWasteItemResponse deactivateItem(
            Long adminId,
            Long wasteItemId
    ) {
        getAdmin(adminId);

        WasteItem wasteItem =
                getWasteItem(wasteItemId);

        wasteItem.deactivate();

        return AdminWasteItemResponse.from(
                wasteItem
        );
    }

    /**
     * 품목의 분리배출 가이드와 체크리스트를 등록하거나 수정
     * 가이드가 없으면 새로 생성하고, 이미 있으면 기존 내용을 수정
     * 체크리스트는 기존 항목을 모두 삭제한 뒤 요청받은 목록으로 전체 교체
     */
    @Transactional
    public RecycleGuideResponse saveGuide(
            Long adminId,
            Long wasteItemId,
            SaveRecycleGuideRequest request
    ) {
        getAdmin(adminId);

        WasteItem wasteItem =
                getWasteItem(wasteItemId);

        RecycleGuide recycleGuide =
                recycleGuideRepository
                        .findByWasteItemId(wasteItemId)
                        .orElse(null);

        if (recycleGuide == null) {
            recycleGuide = RecycleGuide.create(
                    wasteItem,
                    request.summary().trim(),
                    request.disposalMethod().trim(),
                    trimToNull(request.caution())
            );

            recycleGuide =
                    recycleGuideRepository.save(
                            recycleGuide
                    );
        } else {
            recycleGuide.update(
                    request.summary().trim(),
                    request.disposalMethod().trim(),
                    trimToNull(request.caution())
            );
        }

        /**
         * 순서 변경, 항목 추가와 삭제를 쉽게 처리하기 위해 기존 체크리스트를 전체 삭제
         */
        recycleGuideCheckItemRepository
                .deleteAllByRecycleGuide_Id(
                        recycleGuide.getId()
                );

        List<RecycleGuideCheckItem> newCheckItems =
                new ArrayList<>();

        for (
                RecycleGuideCheckItemRequest checkItemRequest
                : request.checkItems()
        ) {
            RecycleGuideCheckItem checkItem =
                    RecycleGuideCheckItem.create(
                            recycleGuide,
                            checkItemRequest.content().trim(),
                            checkItemRequest.sortOrder(),
                            checkItemRequest.required()
                    );

            newCheckItems.add(checkItem);
        }

        List<RecycleGuideCheckItem> savedCheckItems =
                new ArrayList<>(
                        recycleGuideCheckItemRepository
                                .saveAll(newCheckItems)
                );

        /**
         * 관리자 저장 응답도 실제 사용자 조회 순서와 동일하게 sortOrder, id 순으로 정렬
         */
        savedCheckItems.sort(
                Comparator
                        .comparingInt(
                                RecycleGuideCheckItem::getSortOrder
                        )
                        .thenComparing(
                                RecycleGuideCheckItem::getId
                        )
        );

        return RecycleGuideResponse.from(
                recycleGuide,
                savedCheckItems
        );
    }

    /**
     * 관리자 ID로 사용자를 조회하고 실제 ADMIN 권한인지 다시 확인
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
     * 카테고리 ID로 카테고리를 조회
     * 관리자는 비활성 카테고리도 관리할 수 있으므로 active 조건 없이 조회
     */
    private WasteCategory getCategory(
            Long categoryId
    ) {
        return wasteCategoryRepository
                .findById(categoryId)
                .orElseThrow(
                        () -> new CustomException(
                                ErrorCode.WASTE_CATEGORY_NOT_FOUND
                        )
                );
    }

    /**
     * 품목 ID로 품목을 조회
     * 관리자는 비활성 품목도 관리할 수 있으므로 active 조건 없이 조회
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
     * 신규 카테고리 등록 시 코드 중복을 확인
     */
    private void validateDuplicateCategoryCode(
            String code
    ) {
        boolean alreadyExists =
                wasteCategoryRepository
                        .existsByCodeIgnoreCase(code);

        if (alreadyExists) {
            throw new CustomException(
                    ErrorCode.WASTE_CATEGORY_ALREADY_EXISTS
            );
        }
    }

    /**
     * 카테고리 수정 시 현재 카테고리를 제외하고 코드 중복을 확인
     */
    private void validateDuplicateCategoryCodeForUpdate(
            Long categoryId,
            String code
    ) {
        boolean alreadyExists =
                wasteCategoryRepository
                        .existsByCodeIgnoreCaseAndIdNot(
                                code,
                                categoryId
                        );

        if (alreadyExists) {
            throw new CustomException(
                    ErrorCode.WASTE_CATEGORY_ALREADY_EXISTS
            );
        }
    }

    /**
     * 신규 품목 등록 시 동일 카테고리 안에서 품목명 중복을 확인
     */
    private void validateDuplicateWasteItem(
            Long categoryId,
            String name
    ) {
        boolean alreadyExists =
                wasteItemRepository
                        .existsByCategoryIdAndNameIgnoreCase(
                                categoryId,
                                name
                        );

        if (alreadyExists) {
            throw new CustomException(
                    ErrorCode.WASTE_ITEM_ALREADY_EXISTS
            );
        }
    }

    /**
     * 품목 수정 시 현재 품목을 제외하고 동일 카테고리 안의 품목명 중복을 확인
     */
    private void validateDuplicateWasteItemForUpdate(
            Long wasteItemId,
            Long categoryId,
            String name
    ) {
        boolean alreadyExists =
                wasteItemRepository
                        .existsByCategoryIdAndNameIgnoreCaseAndIdNot(
                                categoryId,
                                name,
                                wasteItemId
                        );

        if (alreadyExists) {
            throw new CustomException(
                    ErrorCode.WASTE_ITEM_ALREADY_EXISTS
            );
        }
    }

    /**
     * null 또는 공백 문자열을 null로 변환
     */
    private String trimToNull(
            String value
    ) {
        if (value == null) {
            return null;
        }

        String trimmedValue = value.trim();

        return trimmedValue.isEmpty()
                ? null
                : trimmedValue;
    }

    /**
     * 검색어가 없으면 빈 문자열로 변환
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