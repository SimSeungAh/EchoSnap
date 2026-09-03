package com.echosnap.backend.domain.collectionarea.entity;

/**
 * 수거구역 데이터의 출처입니다.
 *
 * 공공데이터에서 자동 동기화된 구역과
 * 관리자가 직접 관리하는 구역을 구분합니다.
 */
public enum CollectionAreaSourceType {

  /**
   * 행정안전부 생활쓰레기배출정보 공공데이터
   */
  MOIS_HOUSEHOLD_WASTE,

  /**
   * 관리자가 직접 등록하거나 보완한 수거구역
   */
  MANUAL
}