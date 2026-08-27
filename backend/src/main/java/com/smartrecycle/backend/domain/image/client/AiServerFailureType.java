package com.smartrecycle.backend.domain.image.client;

/**
 * Spring Boot가 Python FastAPI AI 서버와
 * 통신하는 과정에서 발생할 수 있는 실패 유형입니다.
 */
public enum AiServerFailureType {

  /**
   * FastAPI 서버가 꺼져 있거나
   * 주소/DNS/포트 문제로 연결 자체가 불가능한 경우
   */
  CONNECTION_FAILED,

  /**
   * 연결 또는 분석 응답을 기다리는 시간이
   * 설정된 timeout을 초과한 경우
   */
  TIMEOUT,

  /**
   * FastAPI가 503 Service Unavailable을 반환한 경우
   *
   * 예:
   * YOLO 모델 파일이 아직 준비되지 않음
   */
  SERVICE_UNAVAILABLE,

  /**
   * FastAPI가 기타 4xx 응답을 반환한 경우
   */
  CLIENT_ERROR,

  /**
   * FastAPI가 기타 5xx 응답을 반환한 경우
   */
  SERVER_ERROR,

  /**
   * HTTP 호출은 완료됐지만
   * 정상적인 YOLO 응답으로 변환할 수 없는 경우
   */
  INVALID_RESPONSE
}