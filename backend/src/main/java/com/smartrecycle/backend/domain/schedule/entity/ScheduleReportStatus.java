package com.smartrecycle.backend.domain.schedule.entity;

/**
 * 주민 일정 제보의 관리자 검토 상태입니다.
 */
public enum ScheduleReportStatus {

  /**
   * 주민이 제보했고
   * 아직 관리자가 최종 검토하지 않은 상태입니다.
   */
  PENDING,

  /**
   * 관리자가 내용을 확인하고
   * 공식 일정 또는 예외 일정에 반영한 상태입니다.
   */
  APPROVED,

  /**
   * 잘못된 정보 또는 반영하기 어려운 제보로
   * 관리자가 거절한 상태입니다.
   */
  REJECTED
}