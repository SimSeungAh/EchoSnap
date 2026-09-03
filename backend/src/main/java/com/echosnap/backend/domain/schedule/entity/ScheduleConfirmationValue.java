package com.echosnap.backend.domain.schedule.entity;

/**
 * 동일 일정 적용 범위 주민이
 * 일정 제보에 남기는 확인 값입니다.
 *
 * 주민의 선호도를 투표하는 기능이 아니라
 * 제보 내용이 실제와 맞는지 확인하는 기능입니다.
 */
public enum ScheduleConfirmationValue {

  /**
   * 제보된 일정 정보가 실제와 동일합니다.
   */
  CONFIRMED,

  /**
   * 제보된 일정 정보와 실제 일정이 다릅니다.
   */
  DIFFERENT
}