package com.echosnap.backend.domain.image.service;

import com.echosnap.backend.domain.image.dto.admin.AdminAiCorrectionDtos;
import com.echosnap.backend.domain.image.dto.response.ImageFileResponse;
import com.echosnap.backend.domain.image.entity.ImageLog;
import com.echosnap.backend.domain.image.entity.ImageReviewStatus;
import com.echosnap.backend.domain.image.repository.ImageLogRepository;
import com.echosnap.backend.domain.image.storage.LocalImageStorageService;
import com.echosnap.backend.domain.user.entity.Role;
import com.echosnap.backend.domain.user.entity.User;
import com.echosnap.backend.domain.user.entity.UserStatus;
import com.echosnap.backend.domain.user.repository.UserRepository;
import com.echosnap.backend.global.exception.CustomException;
import com.echosnap.backend.global.exception.ErrorCode;
import com.echosnap.backend.global.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminAiCorrectionService {

  private static final String JPEG_CONTENT_TYPE =
      "image/jpeg";

  private static final String PNG_CONTENT_TYPE =
      "image/png";

  private final ImageLogRepository
      imageLogRepository;

  private final UserRepository
      userRepository;

  private final LocalImageStorageService
      localImageStorageService;

  /**
   * 관리자 AI 정정 목록 조회
   *
   * reviewStatus == null
   * → 전체
   *
   * PENDING / APPROVED / REJECTED
   * → 해당 상태만 조회
   */
  public PageResponse<
      AdminAiCorrectionDtos.CorrectionResponse
      >
  getCorrections(
      Long adminUserId,
      ImageReviewStatus reviewStatus,
      Pageable pageable
  ) {
    validateAdmin(
        adminUserId
    );

    Page<ImageLog> page;

    if (reviewStatus == null) {

      page =
          imageLogRepository
              .findAllByUserCorrectedWasteItemIsNotNullOrderByUserCorrectedAtDesc(
                  pageable
              );

    } else {

      /*
       * NOT_REQUIRED는 사용자 정정 검수 데이터가 아니므로
       * 관리자 AI 정정 화면에서는 허용하지 않습니다.
       */
      if (
          reviewStatus
              == ImageReviewStatus.NOT_REQUIRED
      ) {
        throw new CustomException(
            ErrorCode.INVALID_INPUT
        );
      }

      page =
          imageLogRepository
              .findAllByReviewStatusOrderByUserCorrectedAtDesc(
                  reviewStatus,
                  pageable
              );
    }

    return PageResponse.from(
        page,
        AdminAiCorrectionDtos
            .CorrectionResponse
            ::from
    );
  }

  /**
   * 관리자 AI 정정 상세 조회
   */
  public AdminAiCorrectionDtos.CorrectionResponse
  getCorrection(
      Long adminUserId,
      Long imageLogId
  ) {
    validateAdmin(
        adminUserId
    );

    ImageLog imageLog =
        getCorrectionImageLog(
            imageLogId
        );

    return AdminAiCorrectionDtos
        .CorrectionResponse
        .from(
            imageLog
        );
  }

  /**
   * 관리자 승인
   */
  @Transactional
  public AdminAiCorrectionDtos.CorrectionResponse
  approve(
      Long adminUserId,
      Long imageLogId,
      String memo
  ) {
    User admin =
        getAdmin(
            adminUserId
        );

    ImageLog imageLog =
        getCorrectionImageLog(
            imageLogId
        );

    validatePending(
        imageLog
    );

    imageLog.approveReview(
        admin,
        normalizeMemo(
            memo
        )
    );

    return AdminAiCorrectionDtos
        .CorrectionResponse
        .from(
            imageLog
        );
  }

  /**
   * 관리자 거절
   */
  @Transactional
  public AdminAiCorrectionDtos.CorrectionResponse
  reject(
      Long adminUserId,
      Long imageLogId,
      String memo
  ) {
    User admin =
        getAdmin(
            adminUserId
        );

    ImageLog imageLog =
        getCorrectionImageLog(
            imageLogId
        );

    validatePending(
        imageLog
    );

    imageLog.rejectReview(
        admin,
        normalizeMemo(
            memo
        )
    );

    return AdminAiCorrectionDtos
        .CorrectionResponse
        .from(
            imageLog
        );
  }

  /**
   * 관리자 검수 화면에서
   * 업로드 이미지를 조회합니다.
   *
   * 일반 사용자의 이미지 API는
   * 본인 소유권만 허용하므로
   * 관리자용 조회 경로를 별도로 둡니다.
   */
  public ImageFileResponse
  getCorrectionImage(
      Long adminUserId,
      Long imageLogId
  ) {
    validateAdmin(
        adminUserId
    );

    ImageLog imageLog =
        getCorrectionImageLog(
            imageLogId
        );

    Resource resource;

    try {

      resource =
          localImageStorageService
              .loadAsResource(
                  imageLog
                      .getStoredFileName()
              );

    } catch (IOException e) {

      throw new CustomException(
          ErrorCode.IMAGE_STORAGE_FAILED
      );
    }

    MediaType mediaType =
        resolveMediaType(
            imageLog
                .getContentType()
        );

    return new ImageFileResponse(
        resource,
        mediaType
    );
  }

  /**
   * 사용자 정정이 실제 존재하는 ImageLog만
   * 관리자 검수 대상으로 인정합니다.
   */
  private ImageLog
  getCorrectionImageLog(
      Long imageLogId
  ) {
    ImageLog imageLog =
        imageLogRepository
            .findById(
                imageLogId
            )
            .orElseThrow(
                () ->
                    new CustomException(
                        ErrorCode.IMAGE_LOG_NOT_FOUND
                    )
            );

    if (
        imageLog
            .getUserCorrectedWasteItem()
            == null
    ) {
      throw new CustomException(
          ErrorCode.INVALID_INPUT
      );
    }

    return imageLog;
  }

  /**
   * 이미 승인/거절된 데이터는
   * 다시 처리하지 않습니다.
   */
  private void validatePending(
      ImageLog imageLog
  ) {
    if (
        imageLog.getReviewStatus()
            != ImageReviewStatus.PENDING
    ) {
      throw new CustomException(
          ErrorCode.INVALID_INPUT
      );
    }
  }

  private void validateAdmin(
      Long userId
  ) {
    getAdmin(
        userId
    );
  }

  private User getAdmin(
      Long userId
  ) {
    User admin =
        userRepository
            .findById(
                userId
            )
            .orElseThrow(
                () ->
                    new CustomException(
                        ErrorCode.USER_NOT_FOUND
                    )
            );

    if (
        admin.getRole()
            != Role.ADMIN
    ) {
      throw new CustomException(
          ErrorCode.FORBIDDEN
      );
    }

    if (
        admin.getStatus()
            != UserStatus.ACTIVE
    ) {
      throw new CustomException(
          ErrorCode.FORBIDDEN
      );
    }

    return admin;
  }

  private String normalizeMemo(
      String memo
  ) {
    if (
        memo == null
            || memo.isBlank()
    ) {
      return null;
    }

    return memo.trim();
  }

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
        ErrorCode.IMAGE_STORAGE_FAILED
    );
  }
}