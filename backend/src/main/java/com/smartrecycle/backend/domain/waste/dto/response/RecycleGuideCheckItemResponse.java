package com.smartrecycle.backend.domain.waste.dto.response;

import com.smartrecycle.backend.domain.waste.entity.RecycleGuideCheckItem;

public record RecycleGuideCheckItemResponse(
        Long id,
        String content,
        int sortOrder,
        boolean required
) {

    /**
     * 체크리스트 엔티티를 사용자 응답 DTO로 변환
     */
    public static RecycleGuideCheckItemResponse from(
            RecycleGuideCheckItem checkItem
    ) {
        return new RecycleGuideCheckItemResponse(
                checkItem.getId(),
                checkItem.getContent(),
                checkItem.getSortOrder(),
                checkItem.isRequired()
        );
    }
}