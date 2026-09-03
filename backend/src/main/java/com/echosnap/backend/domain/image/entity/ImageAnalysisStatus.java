package com.echosnap.backend.domain.image.entity;

/**
 * 이미지 AI 분석 진행 상태입니다.
 */
public enum ImageAnalysisStatus {

  /**
   * 이미지가 서버에 저장됐지만
   * 아직 AI 분석 결과가 기록되지 않은 상태
   */
  UPLOADED,

  /**
   * Flutter의 TensorFlow Lite
   * 1차 분석 결과가 저장된 상태
   */
  MOBILE_ANALYZED,

  /**
   * 모바일 AI 신뢰도가 낮아
   * Python YOLO 재분석을 기다리는 상태
   */
  SERVER_REANALYSIS_PENDING,

  /**
   * Python YOLO 재분석까지
   * 완료된 상태
   */
  SERVER_ANALYZED,

  /**
   * AI 분석 과정에서 오류가 발생한 상태
   */
  ANALYSIS_FAILED
}