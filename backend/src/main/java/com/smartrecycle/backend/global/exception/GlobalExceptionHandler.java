package com.smartrecycle.backend.global.exception;

import com.smartrecycle.backend.global.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  /**
   * SmartRecycle 비즈니스 예외를 처리합니다.
   */
  @ExceptionHandler(CustomException.class)
  public ResponseEntity<ApiResponse<Void>>
  handleCustomException(
      CustomException e
  ) {
    ErrorCode errorCode =
        e.getErrorCode();

    return ResponseEntity
        .status(
            errorCode.getStatus()
        )
        .body(
            ApiResponse.fail(
                errorCode.getCode(),
                errorCode.getMessage()
            )
        );
  }

  /**
   * @Valid 검증 실패를 처리합니다.
   */
  @ExceptionHandler(
      MethodArgumentNotValidException.class
  )
  public ResponseEntity<ApiResponse<Void>>
  handleValidationException(
      MethodArgumentNotValidException e
  ) {
    String message =
        e.getBindingResult()
            .getFieldErrors()
            .get(0)
            .getDefaultMessage();

    return ResponseEntity
        .badRequest()
        .body(
            ApiResponse.fail(
                ErrorCode.INVALID_INPUT.getCode(),
                message
            )
        );
  }

  /**
   * Spring Multipart 자체 제한을 초과한
   * 파일 업로드 요청을 처리합니다.
   *
   * 이 예외는 Controller 또는
   * ImageUploadService에 도달하기 전에
   * 발생할 수 있으므로 전역 예외 처리기에서
   * 별도로 처리합니다.
   */
  @ExceptionHandler(
      MaxUploadSizeExceededException.class
  )
  public ResponseEntity<ApiResponse<Void>>
  handleMaxUploadSizeExceededException(
      MaxUploadSizeExceededException e
  ) {
    ErrorCode errorCode =
        ErrorCode.IMAGE_FILE_TOO_LARGE;

    return ResponseEntity
        .status(
            errorCode.getStatus()
        )
        .body(
            ApiResponse.fail(
                errorCode.getCode(),
                errorCode.getMessage()
            )
        );
  }

  /**
   * 처리되지 않은 예상 밖의 서버 오류를
   * 공통 응답으로 변환합니다.
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>>
  handleException(
      Exception e
  ) {
    return ResponseEntity
        .internalServerError()
        .body(
            ApiResponse.fail(
                ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                ErrorCode.INTERNAL_SERVER_ERROR.getMessage()
            )
        );
  }
}