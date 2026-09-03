package com.echosnap.backend.domain.collectionarea.entity;

/**
 * 지자체 생활쓰레기배출정보에서 사용하는
 * 수거 일정의 폐기물 구분입니다.
 *
 * EchoSnap의 WasteCategory와는 역할이 다릅니다.
 *
 * WasteCategory:
 * 사용자가 검색하거나 AI로 인식하는 품목 분류
 *
 * CollectionWasteType:
 * 지자체 공공데이터에서 제공하는 수거 일정 분류
 */
public enum CollectionWasteType {

  /**
   * 일반 생활쓰레기
   *
   * 공공데이터:
   * LF_WST_*
   */
  LIFE_WASTE,

  /**
   * 음식물쓰레기
   *
   * 공공데이터:
   * FOD_WST_*
   */
  FOOD_WASTE,

  /**
   * 재활용품
   *
   * 공공데이터:
   * RCYCL_*
   */
  RECYCLABLE
}