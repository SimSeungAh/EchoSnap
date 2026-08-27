package com.smartrecycle.backend.domain.image.service;

import com.smartrecycle.backend.domain.image.client.AiServerClientException;
import com.smartrecycle.backend.domain.image.client.AiServerFailureType;
import com.smartrecycle.backend.domain.image.client.YoloAiClient;
import com.smartrecycle.backend.domain.image.dto.external.YoloAnalysisResponse;
import com.smartrecycle.backend.domain.image.dto.response.ServerReanalysisResponse;
import com.smartrecycle.backend.domain.image.entity.ImageAnalysisStatus;
import com.smartrecycle.backend.domain.image.entity.ImageLog;
import com.smartrecycle.backend.domain.image.repository.ImageLogRepository;
import com.smartrecycle.backend.domain.image.storage.LocalImageStorageService;
import com.smartrecycle.backend.domain.waste.entity.WasteItem;
import com.smartrecycle.backend.global.exception.CustomException;
import com.smartrecycle.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

/**
 * 모바일 TensorFlow Lite의 신뢰도가 낮은 이미지를
 * Python YOLO 서버로 재분석하는 Service입니다.
 *
 * 흐름:
 *
 * ImageLog 조회
 * ↓
 * SERVER_REANALYSIS_PENDING 검증
 * ↓
 * 로컬 이미지 읽기
 * ↓
 * FastAPI /analyze 호출
 * ↓
 * YOLO label -> WasteItem 매핑
 * ↓
 * ImageLog 서버 분석 결과 저장
 *
 * AI 서버 연결 실패나 timeout처럼
 * 재시도가 가능한 장애에서는
 * SERVER_REANALYSIS_PENDING 상태를 유지합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServerImageAnalysisService {

  private static final String JPEG_CONTENT_TYPE =
      "image/jpeg";

  private static final String PNG_CONTENT_TYPE =
      "image/png";

  private final ImageLogRepository
      imageLogRepository;

  private final LocalImageStorageService
      localImageStorageService;

  private final YoloAiClient
      yoloAiClient;

  private final AiWasteItemMappingService
      aiWasteItemMappingService;

  /**
   * 로그인한 사용자의 저신뢰도 이미지를
   * Python YOLO로 재분석합니다.
   */
  @Transactional
  public ServerReanalysisResponse reanalyze(
      Long userId,
      Long imageLogId
  ) {
    /*
     * ID만 조회하지 않고
     * 로그인 사용자의 소유권까지 함께 확인합니다.
     */
    ImageLog imageLog =
        imageLogRepository
            .findByIdAndUserId(
                imageLogId,
                userId
            )
            .orElseThrow(
                () ->
                    new CustomException(
                        ErrorCode
                            .IMAGE_LOG_NOT_FOUND
                    )
            );

    validateAnalysisState(
        imageLog
    );

    Resource imageResource =
        loadImage(
            imageLog
        );

    MediaType mediaType =
        resolveMediaType(
            imageLog.getContentType()
        );

    YoloAnalysisResponse yoloResponse;

    try {
      /*
       * 실제 Python FastAPI /analyze 호출
       *
       * 이 호출 전에 ImageLog의 상태를
       * 변경하지 않습니다.
       *
       * 따라서 AI 서버가 잠시 장애 상태여도
       * SERVER_REANALYSIS_PENDING을 유지하며
       * 나중에 다시 시도할 수 있습니다.
       */
      yoloResponse =
          yoloAiClient.analyze(
              imageResource,
              imageLog.getStoredFileName(),
              mediaType
          );

    } catch (AiServerClientException e) {

      log.warn(
          "AI server call failed. "
              + "imageLogId={}, "
              + "failureType={}, "
              + "statusCode={}",
          imageLogId,
          e.getFailureType(),
          e.getStatusCode()
      );

      throw convertAiServerException(
          e
      );
    }

    validateYoloResponse(
        yoloResponse
    );

    /*
     * HTTP 호출과 YOLO 추론은 성공했지만
     * 이미지에서 유효한 품목을 찾지 못한 경우입니다.
     *
     * 이것은 AI 서버 장애가 아니라
     * 실제 분석 실패 결과이므로
     * ANALYSIS_FAILED로 상태를 변경합니다.
     */
    if (!yoloResponse.detected()) {

      imageLog.markAnalysisFailed();

      return ServerReanalysisResponse
          .notDetected(
              imageLog,
              yoloResponse
          );
    }

    /*
     * YOLO classId를
     * WasteItem DB PK로 직접 사용하지 않습니다.
     *
     * label
     * ↓
     * AiWasteItemMapping
     * ↓
     * WasteItem
     */
    WasteItem wasteItem =
        aiWasteItemMappingService
            .findWasteItemByYoloLabel(
                yoloResponse.label()
            )
            .orElseThrow(
                () -> {
                  log.error(
                      "YOLO label mapping not found. "
                          + "imageLogId={}, "
                          + "label={}, "
                          + "modelVersion={}",
                      imageLogId,
                      yoloResponse.label(),
                      yoloResponse.modelVersion()
                  );

                  return new CustomException(
                      ErrorCode
                          .AI_MAPPING_NOT_FOUND
                  );
                }
            );

    /*
     * 서버 AI 분석 결과를 별도로 저장합니다.
     *
     * 기존 모바일 AI 결과는 보존됩니다.
     */
    imageLog.recordServerAnalysis(
        wasteItem,
        yoloResponse.confidence(),
        yoloResponse.modelVersion()
    );

    return ServerReanalysisResponse
        .detected(
            imageLog,
            yoloResponse
        );
  }

  /**
   * SERVER_REANALYSIS_PENDING 상태의 이미지에서만
   * YOLO 서버 재분석을 실행할 수 있습니다.
   */
  private void validateAnalysisState(
      ImageLog imageLog
  ) {
    if (
        imageLog.getAnalysisStatus()
            != ImageAnalysisStatus
            .SERVER_REANALYSIS_PENDING
    ) {
      throw new CustomException(
          ErrorCode
              .AI_REANALYSIS_INVALID_STATE
      );
    }
  }

  /**
   * 저장된 실제 이미지 파일을 읽습니다.
   */
  private Resource loadImage(
      ImageLog imageLog
  ) {
    try {
      return localImageStorageService
          .loadAsResource(
              imageLog.getStoredFileName()
          );

    } catch (IOException e) {
      throw new CustomException(
          ErrorCode.IMAGE_STORAGE_FAILED
      );
    }
  }

  /**
   * ImageLog의 Content-Type을
   * FastAPI multipart 전송용 MediaType으로 변환합니다.
   */
  private MediaType resolveMediaType(
      String contentType
  ) {
    if (
        JPEG_CONTENT_TYPE.equals(
            contentType
        )
    ) {
      return MediaType.IMAGE_JPEG;
    }

    if (
        PNG_CONTENT_TYPE.equals(
            contentType
        )
    ) {
      return MediaType.IMAGE_PNG;
    }

    throw new CustomException(
        ErrorCode.UNSUPPORTED_IMAGE_TYPE
    );
  }

  /**
   * HTTP 요청 자체는 성공했어도
   * FastAPI 응답의 값이 계약에 맞는지
   * 한 번 더 검증합니다.
   */
  private void validateYoloResponse(
      YoloAnalysisResponse response
  ) {
    if (response == null) {
      throw new CustomException(
          ErrorCode
              .AI_SERVER_INVALID_RESPONSE
      );
    }

    /*
     * detected=false에서는
     * label/confidence가 null인 것이 정상입니다.
     */
    if (!response.detected()) {
      return;
    }

    if (
        response.label() == null
            || response.label().isBlank()
    ) {
      throw new CustomException(
          ErrorCode
              .AI_SERVER_INVALID_RESPONSE
      );
    }

    if (
        response.confidence() == null
            || response.confidence() < 0.0
            || response.confidence() > 1.0
    ) {
      throw new CustomException(
          ErrorCode
              .AI_SERVER_INVALID_RESPONSE
      );
    }

    if (
        response.modelVersion() == null
            || response.modelVersion().isBlank()
    ) {
      throw new CustomException(
          ErrorCode
              .AI_SERVER_INVALID_RESPONSE
      );
    }

    if (
        response.detectionCount() <= 0
    ) {
      throw new CustomException(
          ErrorCode
              .AI_SERVER_INVALID_RESPONSE
      );
    }
  }

  /**
   * AI HTTP Client에서 분류한
   * 외부 통신 실패 원인을
   * SmartRecycle의 ErrorCode로 변환합니다.
   */
  private CustomException
  convertAiServerException(
      AiServerClientException exception
  ) {
    AiServerFailureType failureType =
        exception.getFailureType();

    ErrorCode errorCode =
        switch (failureType) {

          case CONNECTION_FAILED ->
              ErrorCode
                  .AI_SERVER_CONNECTION_FAILED;

          case TIMEOUT ->
              ErrorCode
                  .AI_SERVER_TIMEOUT;

          case SERVICE_UNAVAILABLE ->
              ErrorCode
                  .AI_SERVER_UNAVAILABLE;

          case CLIENT_ERROR ->
              ErrorCode
                  .AI_SERVER_REQUEST_REJECTED;

          case SERVER_ERROR ->
              ErrorCode
                  .AI_SERVER_ERROR;

          case INVALID_RESPONSE ->
              ErrorCode
                  .AI_SERVER_INVALID_RESPONSE;
        };

    return new CustomException(
        errorCode
    );
  }
}