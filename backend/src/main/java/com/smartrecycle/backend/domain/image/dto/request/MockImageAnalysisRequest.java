package com.smartrecycle.backend.domain.image.dto.request;

import com.smartrecycle.backend.domain.image.entity.MockAnalysisScenario;
import jakarta.validation.constraints.NotNull;

/**
 * Mock AI 분석 요청입니다.
 *
 * 실제 AI 모델이 없는 개발 단계에서
 * 원하는 분석 상황을 강제로 만들어
 * 앱 전체 흐름을 테스트하기 위해 사용합니다.
 */
public record MockImageAnalysisRequest(

    /**
     * Mock AI가 반환할 폐기물 품목입니다.
     *
     * ANALYSIS_FAILED 시나리오에서는
     * 실제 분석 결과로 사용되지 않습니다.
     */
    Long wasteItemId,

    /**
     * 테스트할 AI 분석 상황
     */
    @NotNull(
        message = "Mock AI 시나리오는 필수입니다."
    )
    MockAnalysisScenario scenario

) {
}