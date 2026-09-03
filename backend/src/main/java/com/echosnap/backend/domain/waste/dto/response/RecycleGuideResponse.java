package com.echosnap.backend.domain.waste.dto.response;

import com.echosnap.backend.domain.waste.entity.RecycleGuide;
import com.echosnap.backend.domain.waste.entity.RecycleGuideCheckItem;

import java.util.List;

public record RecycleGuideResponse(
        Long id,
        String summary,
        String disposalMethod,
        String caution,
        List<RecycleGuideCheckItemResponse> checkItems
) {

    /**
     * 분리배출 가이드와 체크리스트 엔티티를 사용자용 가이드 응답 DTO로 변환
     */
    public static RecycleGuideResponse from(
            RecycleGuide recycleGuide,
            List<RecycleGuideCheckItem> checkItems
    ) {
        return new RecycleGuideResponse(
                recycleGuide.getId(),
                recycleGuide.getSummary(),
                recycleGuide.getDisposalMethod(),
                recycleGuide.getCaution(),
                checkItems.stream()
                        .map(RecycleGuideCheckItemResponse::from)
                        .toList()
        );
    }
}