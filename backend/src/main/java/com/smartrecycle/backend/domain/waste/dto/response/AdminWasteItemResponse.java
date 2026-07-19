package com.smartrecycle.backend.domain.waste.dto.response;

import com.smartrecycle.backend.domain.waste.entity.WasteItem;

import java.time.LocalDateTime;

public record AdminWasteItemResponse(
        Long id,
        String name,
        String searchKeywords,
        String imageUrl,
        WasteCategorySummaryResponse category,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    /**
     * 폐기물 품목 엔티티를 관리자용 품목 응답 DTO로 변환
     * 일반 사용자용 응답과 달리 검색 키워드, 활성 상태, 생성·수정 시각을 포함
     */
    public static AdminWasteItemResponse from(
            WasteItem wasteItem
    ) {
        return new AdminWasteItemResponse(
                wasteItem.getId(),
                wasteItem.getName(),
                wasteItem.getSearchKeywords(),
                wasteItem.getImageUrl(),
                WasteCategorySummaryResponse.from(
                        wasteItem.getCategory()
                ),
                wasteItem.isActive(),
                wasteItem.getCreatedAt(),
                wasteItem.getUpdatedAt()
        );
    }
}