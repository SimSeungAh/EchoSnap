package com.smartrecycle.backend.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

  /*
   * 공통
   */
  INVALID_INPUT(
          HttpStatus.BAD_REQUEST,
          "COMMON_001",
          "잘못된 입력입니다."
  ),

  INTERNAL_SERVER_ERROR(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "SERVER_001",
          "서버 내부 오류가 발생했습니다."
  ),

  /*
   * 인증 및 권한
   */
  UNAUTHORIZED(
          HttpStatus.UNAUTHORIZED,
          "AUTH_001",
          "인증이 필요합니다."
  ),

  FORBIDDEN(
          HttpStatus.FORBIDDEN,
          "AUTH_002",
          "접근 권한이 없습니다."
  ),

  /*
   * 사용자
   */
  EMAIL_ALREADY_EXISTS(
          HttpStatus.BAD_REQUEST,
          "USER_001",
          "이미 사용 중인 이메일입니다."
  ),

  USER_NOT_FOUND(
          HttpStatus.NOT_FOUND,
          "USER_002",
          "사용자를 찾을 수 없습니다."
  ),

  PASSWORD_NOT_MATCH(
          HttpStatus.UNAUTHORIZED,
          "USER_003",
          "비밀번호가 일치하지 않습니다."
  ),

  USER_APARTMENT_NOT_SET(
          HttpStatus.BAD_REQUEST,
          "USER_004",
          "거주 아파트가 설정되지 않았습니다."
  ),

  /*
   * 토큰
   */
  INVALID_TOKEN(
          HttpStatus.UNAUTHORIZED,
          "TOKEN_001",
          "유효하지 않은 토큰입니다."
  ),

  EXPIRED_TOKEN(
          HttpStatus.UNAUTHORIZED,
          "TOKEN_002",
          "만료된 토큰입니다."
  ),

  UNSUPPORTED_TOKEN(
          HttpStatus.UNAUTHORIZED,
          "TOKEN_003",
          "지원하지 않는 토큰입니다."
  ),

  EMPTY_TOKEN(
          HttpStatus.UNAUTHORIZED,
          "TOKEN_004",
          "토큰이 존재하지 않습니다."
  ),

  /*
   * 아파트
   */
  APARTMENT_NOT_FOUND(
          HttpStatus.NOT_FOUND,
          "APARTMENT_001",
          "아파트를 찾을 수 없습니다."
  ),

  APARTMENT_ALREADY_EXISTS(
          HttpStatus.CONFLICT,
          "APARTMENT_002",
          "이미 등록된 건물관리번호입니다."
  ),

  INVALID_APARTMENT_STATUS(
          HttpStatus.CONFLICT,
          "APARTMENT_003",
          "현재 상태에서는 아파트 승인 또는 거절 처리를 할 수 없습니다."
  ),

  APARTMENT_NOT_APPROVED(
          HttpStatus.BAD_REQUEST,
          "APARTMENT_004",
          "승인된 아파트만 선택할 수 있습니다."
  ),

  /*
   * 폐기물 품목 및 분리배출 가이드
   */
  WASTE_ITEM_NOT_FOUND(
          HttpStatus.NOT_FOUND,
          "WASTE_001",
          "폐기물 품목을 찾을 수 없습니다."
  ),

  WASTE_ITEM_ALREADY_EXISTS(
          HttpStatus.CONFLICT,
          "WASTE_002",
          "같은 카테고리에 이미 등록된 폐기물 품목입니다."
  ),

  WASTE_CATEGORY_NOT_FOUND(
          HttpStatus.NOT_FOUND,
          "WASTE_003",
          "폐기물 카테고리를 찾을 수 없습니다."
  ),

  WASTE_CATEGORY_ALREADY_EXISTS(
          HttpStatus.CONFLICT,
          "WASTE_004",
          "이미 등록된 폐기물 카테고리 코드입니다."
  ),

  /*
   * 아파트별 배출 일정
   */
  RECYCLE_SCHEDULE_NOT_FOUND(
          HttpStatus.NOT_FOUND,
          "SCHEDULE_001",
          "배출 일정을 찾을 수 없습니다."
  ),

  RECYCLE_SCHEDULE_ALREADY_EXISTS(
          HttpStatus.CONFLICT,
          "SCHEDULE_002",
          "동일한 아파트와 품목에 중복된 배출 일정이 존재합니다."
  ),

  INVALID_RECYCLE_SCHEDULE(
          HttpStatus.BAD_REQUEST,
          "SCHEDULE_003",
          "배출 일정 정보가 올바르지 않습니다."
  ),

  INVALID_RECYCLE_SCHEDULE_TIME(
          HttpStatus.BAD_REQUEST,
          "SCHEDULE_004",
          "배출 종료 시간은 시작 시간보다 늦어야 합니다."
  );

  private final HttpStatus status;
  private final String code;
  private final String message;

  ErrorCode(
          HttpStatus status,
          String code,
          String message
  ) {
    this.status = status;
    this.code = code;
    this.message = message;
  }
}