package com.smartrecycle.backend.domain.image.entity;

/**
 * AI 이미지 분석 이력에 대한
 * 관리자 검수 상태입니다.
 */
public enum ImageReviewStatus {

  /**
   * 관리자 검수가 필요하지 않은 일반 분석 결과
   */
  NOT_REQUIRED,

  /**
   * 사용자 수정 등으로 인해
   * 관리자 검수가 필요한 상태
   */
  PENDING,

  /**
   * 관리자가 학습/분석 데이터로
   * 사용할 수 있다고 승인한 상태
   */
  APPROVED,

  /**
   * 잘못된 이미지, 부적절한 데이터 등으로
   * 관리자가 사용하지 않기로 결정한 상태
   */
  REJECTED
}