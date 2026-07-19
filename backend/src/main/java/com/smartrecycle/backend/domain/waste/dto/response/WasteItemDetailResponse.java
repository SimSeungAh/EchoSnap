package com.smartrecycle.backend.domain.waste.dto.response;

import com.smartrecycle.backend.domain.waste.entity.RecycleGuide;
import com.smartrecycle.backend.domain.waste.entity.RecycleGuideCheckItem;
import com.smartrecycle.backend.domain.waste.entity.WasteItem;

import java.util.List;

public record WasteItemDetailResponse(
        Long id,
        String name,
        String imageUrl,
        WasteCategorySummaryResponse category,
        RecycleGuideResponse guide
) {

    /**
     * 폐기물 품목, 분리배출 가이드, 체크리스트를 사용자용 품목 상세 응답으로 변환
     * 품목에 가이드가 아직 등록되지 않았다면 guide는 null로 반환
     */
    public static WasteItemDetailResponse from(
            WasteItem wasteItem,
            RecycleGuide recycleGuide,
            List<RecycleGuideCheckItem> checkItems
    ) {
        RecycleGuideResponse guideResponse = null;

        if (recycleGuide != null) {
            guideResponse = RecycleGuideResponse.from(
                    recycleGuide,
                    checkItems
            );
        }

        return new WasteItemDetailResponse(
                wasteItem.getId(),
                wasteItem.getName(),
                wasteItem.getImageUrl(),
                WasteCategorySummaryResponse.from(
                        wasteItem.getCategory()
                ),
                guideResponse
        );
    }
}