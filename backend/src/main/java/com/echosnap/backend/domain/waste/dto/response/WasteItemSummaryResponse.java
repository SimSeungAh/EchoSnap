package com.echosnap.backend.domain.waste.dto.response;

import com.echosnap.backend.domain.waste.entity.WasteItem;

public record WasteItemSummaryResponse(
        Long id,
        String name,
        String imageUrl,
        WasteCategorySummaryResponse category
) {

    /**
     * WasteItem 엔티티를 품목 목록용 응답 DTO로 변환
     */
    public static WasteItemSummaryResponse from(
            WasteItem wasteItem
    ) {
        return new WasteItemSummaryResponse(
                wasteItem.getId(),
                wasteItem.getName(),
                wasteItem.getImageUrl(),
                WasteCategorySummaryResponse.from(
                        wasteItem.getCategory()
                )
        );
    }
}

