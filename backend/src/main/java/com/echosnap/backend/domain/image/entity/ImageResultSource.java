package com.echosnap.backend.domain.image.entity;

/**
 * 현재 사용자에게 보여주는
 * 최종 폐기물 품목 결과가
 * 어디에서 결정되었는지를 나타냅니다.
 */
public enum ImageResultSource {

  /**
   * 아직 분석 결과가 없음
   *
   * 예:
   * UPLOADED
   * ANALYSIS_FAILED
   */
  NONE,

  /**
   * Flutter TensorFlow Lite
   * 1차 분석 결과
   */
  MOBILE_AI,

  /**
   * Python YOLO
   * 서버 재분석 결과
   */
  SERVER_AI,

  /**
   * 사용자가 AI 결과를 직접 수정한 결과
   */
  USER_CORRECTION
}