package com.echosnap.backend.domain.user.entity;

/**
 * 사용자의 거주지 일정 적용 유형입니다.
 *
 * 실제 건축물의 법적 분류보다
 * EchoSnap에서 어떤 배출 일정 기준을 적용할지에 따라 구분합니다.
 */
public enum ResidenceType {

  /**
   * 관리사무소 또는 단지 자체 배출 일정이 존재하는 거주지
   *
   * 예:
   * - 아파트
   * - 오피스텔
   * - 자체 분리수거 일정을 운영하는 공동주택
   *
   * 기존 Apartment 및 RecycleSchedule을 기준으로 일정을 조회합니다.
   */
  MANAGED_COMPLEX,

  /**
   * 개별 단지 일정이 아닌
   * 주소 기반 지자체/수거구역 일정을 적용하는 거주지
   *
   * 예:
   * - 단독주택
   * - 다가구주택
   * - 지역 수거 일정을 따르는 연립/다세대주택 등
   *
   * 이후 주소 API와 지자체 공공데이터 연동을 통해
   * 해당 지역의 배출 일정을 조회합니다.
   */
  GENERAL_HOUSING
}