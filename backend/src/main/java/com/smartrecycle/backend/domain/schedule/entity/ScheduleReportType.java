package com.smartrecycle.backend.domain.schedule.entity;

/**
 * 주민이 등록할 수 있는 일정 제보 종류입니다.
 */
public enum ScheduleReportType {

  /**
   * 아직 공식 일정이 없는 공동주택 또는 수거구역에
   * 최초 일정을 제보합니다.
   *
   * 관리자 승인 후 공식 정기 일정이 생성됩니다.
   */
  INITIAL_SCHEDULE,

  /**
   * 현재 등록된 공식 정기 일정이
   * 실제와 다른 경우 수정 내용을 제보합니다.
   *
   * 관리자 승인 후 기존 정기 일정이 수정됩니다.
   */
  SCHEDULE_CORRECTION,

  /**
   * 특정 날짜에만 일정이 변경되거나
   * 수거가 중단되는 경우 제보합니다.
   *
   * 관리자 승인 후 ScheduleException이 생성됩니다.
   */
  TEMPORARY_CHANGE
}