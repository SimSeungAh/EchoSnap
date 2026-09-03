package com.echosnap.backend.domain.image.dto.external;

/**
 * Python FastAPI /analyze 응답입니다.
 *
 * FastAPI AnalyzeResponse와
 * 필드명을 동일하게 유지합니다.
 */
public record YoloAnalysisResponse(

    /**
     * YOLO가 분석 결과를 찾았는지 여부
     */
    boolean detected,

    /**
     * YOLO 모델 내부 class index
     *
     * 예:
     * 0
     * 1
     * 2
     */
    Integer classId,

    /**
     * YOLO 모델의 class label
     *
     * 예:
     * plastic_bottle
     * paper_cup
     */
    String label,

    /**
     * YOLO 분석 신뢰도
     *
     * 0.0 ~ 1.0
     */
    Double confidence,

    /**
     * Detection 모델일 경우
     * 이미지에서 감지된 객체 수
     *
     * Classification 모델은 1
     */
    int detectionCount,

    /**
     * FastAPI 서버에서 사용한
     * YOLO 모델 버전
     */
    String modelVersion

) {
}