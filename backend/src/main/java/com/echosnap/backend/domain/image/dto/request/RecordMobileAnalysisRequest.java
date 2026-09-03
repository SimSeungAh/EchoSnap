package com.echosnap.backend.domain.image.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Flutter TensorFlow Lite가 수행한
 * 모바일 AI 1차 분석 결과입니다.
 *
 * AI 모델의 classId를 DB의 WasteItem ID로
 * 직접 사용하지 않습니다.
 *
 * Flutter에서는 실제 모델 label을 전달하고,
 * Spring Boot에서 해당 label을 WasteItem으로
 * 매핑합니다.
 */
public record RecordMobileAnalysisRequest(

    /**
     * TensorFlow Lite가 예측한 실제 모델 label
     *
     * 예:
     * cardboard_box
     * pet_bottle
     * plastic_container
     * can
     * glass_bottle
     * styrofoam
     */
    @NotBlank(
        message = "모바일 AI 모델 라벨은 필수입니다."
    )
    @Size(
        max = 150,
        message = "모바일 AI 모델 라벨은 150자 이하여야 합니다."
    )
    String modelLabel,

    /**
     * 모델 신뢰도
     *
     * 범위:
     * 0.0 ~ 1.0
     */
    @NotNull(
        message = "AI 신뢰도는 필수입니다."
    )
    @DecimalMin(
        value = "0.0",
        inclusive = true,
        message = "AI 신뢰도는 0 이상이어야 합니다."
    )
    @DecimalMax(
        value = "1.0",
        inclusive = true,
        message = "AI 신뢰도는 1 이하여야 합니다."
    )
    Double confidence,

    /**
     * Flutter에 탑재된
     * TensorFlow Lite 모델 버전
     *
     * 예:
     * echosnap-tflite-v1
     */
    @NotBlank(
        message = "모바일 AI 모델 버전은 필수입니다."
    )
    @Size(
        max = 100,
        message = "모델 버전은 100자 이하여야 합니다."
    )
    String modelVersion

) {
}