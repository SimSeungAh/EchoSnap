package com.smartrecycle.backend.domain.image.entity;

/**
 * 실제 AI 모델 없이
 * 다양한 분석 상황을 테스트하기 위한 Mock 시나리오입니다.
 */
public enum MockAnalysisScenario {

  /**
   * 높은 신뢰도 분석 성공
   *
   * 예:
   * confidence = 0.92
   */
  HIGH_CONFIDENCE,

  /**
   * 낮은 신뢰도 분석 성공
   *
   * 서버 YOLO 재분석이 필요한 상황을
   * 테스트하기 위해 사용합니다.
   *
   * 예:
   * confidence = 0.45
   */
  LOW_CONFIDENCE,

  /**
   * AI가 이미지를 정상적으로
   * 분류하지 못한 상황을 테스트합니다.
   */
  ANALYSIS_FAILED
}