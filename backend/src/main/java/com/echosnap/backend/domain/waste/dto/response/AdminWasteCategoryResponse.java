package com.echosnap.backend.domain.waste.dto.response;

import com.echosnap.backend.domain.waste.entity.WasteCategory;

import java.time.LocalDateTime;

public record AdminWasteCategoryResponse(
        Long id,
        String code,
        String name,
        String description,
        int sortOrder,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    /**
     * 폐기물 카테고리 엔티티를 관리자용 응답 DTO로 변환
     * 일반 사용자용 응답과 달리 활성 여부와 생성·수정 시각을 포함
     */
    public static AdminWasteCategoryResponse from(
            WasteCategory category
    ) {
        return new AdminWasteCategoryResponse(
                category.getId(),
                category.getCode(),
                category.getName(),
                category.getDescription(),
                category.getSortOrder(),
                category.isActive(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }
}