package com.echosnap.backend.domain.image.client;

/**
 * Python FastAPI AI 서버 호출 과정에서
 * 발생한 연동 예외입니다.
 *
 * HTTP Client 계층에서는
 * EchoSnap의 ErrorCode를 직접 결정하지 않고,
 * 통신 실패 원인만 분류합니다.
 *
 * 이후 Service 계층에서 이 실패 유형을
 * 서비스 ErrorCode로 변환합니다.
 */
public class AiServerClientException
    extends RuntimeException {

  private final AiServerFailureType
      failureType;

  private final Integer
      statusCode;

  public AiServerClientException(
      AiServerFailureType failureType,
      String message
  ) {
    super(message);

    this.failureType =
        failureType;

    this.statusCode =
        null;
  }

  public AiServerClientException(
      AiServerFailureType failureType,
      String message,
      Throwable cause
  ) {
    super(
        message,
        cause
    );

    this.failureType =
        failureType;

    this.statusCode =
        null;
  }

  public AiServerClientException(
      AiServerFailureType failureType,
      Integer statusCode,
      String message,
      Throwable cause
  ) {
    super(
        message,
        cause
    );

    this.failureType =
        failureType;

    this.statusCode =
        statusCode;
  }

  public AiServerFailureType
  getFailureType() {
    return failureType;
  }

  public Integer getStatusCode() {
    return statusCode;
  }
}