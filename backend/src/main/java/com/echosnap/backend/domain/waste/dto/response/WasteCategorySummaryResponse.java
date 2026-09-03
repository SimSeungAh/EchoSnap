package com.echosnap.backend.domain.waste.dto.response;

import com.echosnap.backend.domain.waste.entity.WasteCategory;

public record WasteCategorySummaryResponse(
        Long id,
        String code,
        String name
) {

    /**
     * WasteCategory 엔티티를 품목 응답 내부에서 사용할 간단한 카테고리 DTO로 변환
     */
    public static WasteCategorySummaryResponse from(
            WasteCategory category
    ) {
        return new WasteCategorySummaryResponse(
                category.getId(),
                category.getCode(),
                category.getName()
        );
    }
}