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

  USER_RESIDENCE_NOT_SET(
      HttpStatus.BAD_REQUEST,
      "USER_005",
      "일반주택 거주지가 설정되지 않았습니다."
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
   * 폐기물
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
   * 공식 배출 일정
   */
  RECYCLE_SCHEDULE_NOT_FOUND(
      HttpStatus.NOT_FOUND,
      "SCHEDULE_001",
      "배출 일정을 찾을 수 없습니다."
  ),

  RECYCLE_SCHEDULE_ALREADY_EXISTS(
      HttpStatus.CONFLICT,
      "SCHEDULE_002",
      "동일한 거주 범위와 품목에 충돌하는 배출 일정이 존재합니다."
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
  ),

  SCHEDULE_EXCEPTION_ALREADY_EXISTS(
      HttpStatus.CONFLICT,
      "SCHEDULE_005",
      "해당 날짜에 이미 공식 예외 일정이 존재합니다."
  ),

  /*
   * 주민 일정 제보
   */
  SCHEDULE_REPORT_NOT_FOUND(
      HttpStatus.NOT_FOUND,
      "REPORT_001",
      "일정 제보를 찾을 수 없습니다."
  ),

  INVALID_SCHEDULE_REPORT(
      HttpStatus.BAD_REQUEST,
      "REPORT_002",
      "일정 제보 정보가 올바르지 않습니다."
  ),

  SCHEDULE_REPORT_TARGET_MISMATCH(
      HttpStatus.FORBIDDEN,
      "REPORT_003",
      "현재 거주지에 적용되는 일정만 제보할 수 있습니다."
  ),

  SCHEDULE_REPORT_OFFICIAL_SCHEDULE_EXISTS(
      HttpStatus.CONFLICT,
      "REPORT_004",
      "이미 공식 일정이 존재합니다. 일정 정정 제보를 이용해주세요."
  ),

  SCHEDULE_REPORT_REFERENCE_REQUIRED(
      HttpStatus.BAD_REQUEST,
      "REPORT_005",
      "기존 공식 일정 정보가 필요합니다."
  ),

  SCHEDULE_REPORT_REFERENCE_NOT_FOUND(
      HttpStatus.NOT_FOUND,
      "REPORT_006",
      "참조할 공식 일정을 찾을 수 없습니다."
  ),

  SCHEDULE_REPORT_COLLECTION_AREA_NOT_MATCHED(
      HttpStatus.BAD_REQUEST,
      "REPORT_007",
      "현재 주소에 해당 폐기물 종류의 수거구역이 연결되어 있지 않습니다."
  ),

  INVALID_SCHEDULE_REPORT_TIME(
      HttpStatus.BAD_REQUEST,
      "REPORT_008",
      "제보한 배출 시간 정보가 올바르지 않습니다."
  ),

  SCHEDULE_REPORT_NOT_PENDING(
      HttpStatus.CONFLICT,
      "REPORT_009",
      "검토가 완료된 일정 제보에는 더 이상 처리할 수 없습니다."
  ),

  SCHEDULE_REPORT_SELF_CONFIRMATION_NOT_ALLOWED(
      HttpStatus.BAD_REQUEST,
      "REPORT_010",
      "자신이 등록한 일정 제보에는 확인할 수 없습니다."
  ),

  SCHEDULE_REPORT_CONFIRMATION_SCOPE_MISMATCH(
      HttpStatus.FORBIDDEN,
      "REPORT_011",
      "같은 일정 적용 범위의 주민만 이 제보를 확인할 수 있습니다."
  ),

  SCHEDULE_REPORT_PUBLIC_REASON_REQUIRED(
      HttpStatus.BAD_REQUEST,
      "REPORT_012",
      "일시 변경 승인 시 사용자에게 안내할 사유는 필수입니다."
  ),

  /*
   * 주소 검색
   */
  INVALID_ADDRESS_SEARCH_CONDITION(
      HttpStatus.BAD_REQUEST,
      "ADDRESS_001",
      "주소 검색 조건이 올바르지 않습니다."
  ),

  ADDRESS_SEARCH_API_ERROR(
      HttpStatus.BAD_GATEWAY,
      "ADDRESS_002",
      "주소 검색 외부 API 호출에 실패했습니다."
  ),

  /*
   * 지자체 공공데이터
   */
  PUBLIC_DATA_API_ERROR(
      HttpStatus.BAD_GATEWAY,
      "PUBLIC_DATA_001",
      "지자체 공공데이터 API 호출에 실패했습니다."
  ),

  PUBLIC_DATA_INVALID_RESPONSE(
      HttpStatus.BAD_GATEWAY,
      "PUBLIC_DATA_002",
      "지자체 공공데이터 응답 형식이 올바르지 않습니다."
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