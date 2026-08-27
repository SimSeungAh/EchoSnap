package com.smartrecycle.backend.domain.image.service;

import com.smartrecycle.backend.domain.image.dto.request.ApproveImageReviewRequest;
import com.smartrecycle.backend.domain.image.dto.request.RejectImageReviewRequest;
import com.smartrecycle.backend.domain.image.dto.response.AdminImageReviewResponse;
import com.smartrecycle.backend.domain.image.dto.response.ImageFileResponse;
import com.smartrecycle.backend.domain.image.entity.ImageLog;
import com.smartrecycle.backend.domain.image.entity.ImageReviewStatus;
import com.smartrecycle.backend.domain.image.repository.ImageLogRepository;
import com.smartrecycle.backend.domain.image.storage.LocalImageStorageService;
import com.smartrecycle.backend.domain.user.entity.Role;
import com.smartrecycle.backend.domain.user.entity.User;
import com.smartrecycle.backend.domain.user.repository.UserRepository;
import com.smartrecycle.backend.global.exception.CustomException;
import com.smartrecycle.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

/**
 * 관리자 AI 이미지 검수 Service입니다.
 *
 * 관리자 기능은 SecurityConfig뿐 아니라
 * Service에서도 ADMIN 권한을 다시 검증합니다.
 */
@Service
@RequiredArgsConstructor
public class AdminImageReviewService {

  private static final String JPEG_CONTENT_TYPE =
      "image/jpeg";

  private static final String PNG_CONTENT_TYPE =
      "image/png";

  private final UserRepository
      userRepository;

  private final ImageLogRepository
      imageLogRepository;

  private final LocalImageStorageService
      localImageStorageService;

  /**
   * 검수 대기 이미지를 오래된 순서로 조회합니다.
   */
  @Transactional(readOnly = true)
  public Page<AdminImageReviewResponse>
  getPendingReviews(
      Long adminId,
      Pageable pageable
  ) {
    getAdmin(
        adminId
    );

    return imageLogRepository
        .findAllByReviewStatusOrderByCreatedAtAsc(
            ImageReviewStatus.PENDING,
            pageable
        )
        .map(
            AdminImageReviewResponse::from
        );
  }

  /**
   * 검수 상세 조회
   */
  @Transactional(readOnly = true)
  public AdminImageReviewResponse
  getReviewDetail(
      Long adminId,
      Long imageLogId
  ) {
    getAdmin(
        adminId
    );

    ImageLog imageLog =
        getImageLog(
            imageLogId
        );

    return AdminImageReviewResponse.from(
        imageLog
    );
  }

  /**
   * 관리자 검수용 원본 이미지 조회
   */
  @Transactional(readOnly = true)
  public ImageFileResponse
  getReviewImageFile(
      Long adminId,
      Long imageLogId
  ) {
    getAdmin(
        adminId
    );

    ImageLog imageLog =
        getImageLog(
            imageLogId
        );

    Resource resource;

    try {
      resource =
          localImageStorageService
              .loadAsResource(
                  imageLog.getStoredFileName()
              );

    } catch (IOException e) {
      throw new CustomException(
          ErrorCode.IMAGE_STORAGE_FAILED
      );
    }

    MediaType mediaType =
        resolveMediaType(
            imageLog.getContentType()
        );

    return new ImageFileResponse(
        resource,
        mediaType
    );
  }

  /**
   * 사용자 수정 데이터를 승인합니다.
   *
   * 승인된 데이터는 이후
   * AI 학습 후보로 활용할 수 있는
   * 검수 완료 데이터가 됩니다.
   */
  @Transactional
  public AdminImageReviewResponse
  approveReview(
      Long adminId,
      Long imageLogId,
      ApproveImageReviewRequest request
  ) {
    User admin =
        getAdmin(
            adminId
        );

    ImageLog imageLog =
        getImageLog(
            imageLogId
        );

    validatePendingReview(
        imageLog
    );

    /*
     * 관리자 검수 대상은
     * 사용자 수정값이 있는 ImageLog여야 합니다.
     *
     * 정상적인 흐름에서는
     * correctByUser()가 reviewStatus를
     * PENDING으로 만들기 때문에
     * PENDING인데 사용자 수정값이 없는 상황은
     * 비정상 데이터로 봅니다.
     */
    validateUserCorrectionExists(
        imageLog
    );

    imageLog.approveReview(
        admin,
        trimToNull(
            request.reviewNote()
        )
    );

    /*
     * ImageLog는 영속 상태이므로
     * 별도 save() 없이 Dirty Checking으로
     * UPDATE됩니다.
     */
    return AdminImageReviewResponse.from(
        imageLog
    );
  }

  /**
   * 사용자 수정 데이터를 거절합니다.
   *
   * RejectImageReviewRequest에서
   * reviewNote를 필수 검증하고 있으므로
   * 거절 이유가 반드시 남습니다.
   */
  @Transactional
  public AdminImageReviewResponse
  rejectReview(
      Long adminId,
      Long imageLogId,
      RejectImageReviewRequest request
  ) {
    User admin =
        getAdmin(
            adminId
        );

    ImageLog imageLog =
        getImageLog(
            imageLogId
        );

    validatePendingReview(
        imageLog
    );

    validateUserCorrectionExists(
        imageLog
    );

    imageLog.rejectReview(
        admin,
        request.reviewNote()
            .trim()
    );

    return AdminImageReviewResponse.from(
        imageLog
    );
  }

  /**
   * 검수가 완료된 데이터를
   * 다시 승인/거절하지 못하도록 합니다.
   *
   * 현재 Image 전용 상태 오류 코드를
   * 별도로 추가하지 않고
   * 기존 공통 입력 오류를 사용합니다.
   *
   * 필요하면 이후 통합 예외 정리 단계에서
   * IMAGE 전용 코드로 세분화할 수 있습니다.
   */
  private void validatePendingReview(
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

  /**
   * AI 학습 후보 검수는
   * 사용자 수정값이 실제로 존재하는 데이터만
   * 처리하도록 방어적으로 확인합니다.
   */
  private void validateUserCorrectionExists(
      ImageLog imageLog
  ) {
    if (
        imageLog.getUserCorrectedWasteItem()
            == null
    ) {
      throw new CustomException(
          ErrorCode.INVALID_INPUT
      );
    }
  }

  /**
   * ImageLog 조회
   */
  private ImageLog getImageLog(
      Long imageLogId
  ) {
    return imageLogRepository
        .findById(
            imageLogId
        )
        .orElseThrow(
            () ->
                new CustomException(
                    ErrorCode
                        .IMAGE_LOG_NOT_FOUND
                )
        );
  }

  /**
   * Service 계층 관리자 권한 검증
   */
  private User getAdmin(
      Long adminId
  ) {
    User user =
        userRepository
            .findById(
                adminId
            )
            .orElseThrow(
                () ->
                    new CustomException(
                        ErrorCode.USER_NOT_FOUND
                    )
            );

    if (
        user.getRole()
            != Role.ADMIN
    ) {
      throw new CustomException(
          ErrorCode.FORBIDDEN
      );
    }

    return user;
  }

  /**
   * 빈 문자열을 DB에 그대로 저장하지 않고
   * null로 정규화합니다.
   */
  private String trimToNull(
      String value
  ) {
    if (
        value == null
            || value.isBlank()
    ) {
      return null;
    }

    return value.trim();
  }

  /**
   * DB Content-Type을 HTTP MediaType으로 변환합니다.
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
        ErrorCode.IMAGE_STORAGE_FAILED
    );
  }
}