package com.echosnap.backend.domain.schedule.entity;

/**
 * 일반주택 공식 일정의 현재 일정 규칙 출처입니다.
 *
 * PUBLIC_DATA:
 * 행정안전부 생활쓰레기배출정보 공공데이터에서
 * 가져온 정기 일정입니다.
 *
 * ADMIN_APPROVED_REPORT:
 * 주민 일정 제보를 관리자가 검토하고 승인하여
 * 공식 일정으로 반영한 값입니다.
 */
public enum CollectionAreaScheduleSourceType {

  PUBLIC_DATA,

  ADMIN_APPROVED_REPORT
}