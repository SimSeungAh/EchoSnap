package com.smartrecycle.backend.domain.waste.dto.response;

import com.smartrecycle.backend.domain.waste.entity.WasteCategory;

public record WasteCategoryResponse(
        Long id,
        String code,
        String name,
        String description,
        int sortOrder
) {

    /**
     * WasteCategory 엔티티를 일반 사용자용 응답 DTO로 변환
     */
    public static WasteCategoryResponse from(
            WasteCategory category
    ) {
        return new WasteCategoryResponse(
                category.getId(),
                category.getCode(),
                category.getName(),
                category.getDescription(),
                category.getSortOrder()
        );
    }
}