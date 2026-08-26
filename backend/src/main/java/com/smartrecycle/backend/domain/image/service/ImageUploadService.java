package com.smartrecycle.backend.domain.image.service;

import com.smartrecycle.backend.domain.image.dto.response.ImageUploadResponse;
import com.smartrecycle.backend.domain.image.entity.ImageLog;
import com.smartrecycle.backend.domain.image.repository.ImageLogRepository;
import com.smartrecycle.backend.domain.image.storage.LocalImageStorageService;
import com.smartrecycle.backend.domain.image.storage.StoredImageFile;
import com.smartrecycle.backend.domain.user.entity.User;
import com.smartrecycle.backend.domain.user.repository.UserRepository;
import com.smartrecycle.backend.global.config.ImageStorageProperties;
import com.smartrecycle.backend.global.exception.CustomException;
import com.smartrecycle.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

/**
 * 이미지 업로드 비즈니스 로직입니다.
 *
 * 담당 역할:
 *
 * - 로그인 사용자 확인
 * - 빈 파일 검증
 * - 파일 크기 검증
 * - Content-Type 검증
 * - 실제 이미지 시그니처 검증
 * - 로컬 파일 저장
 * - ImageLog 생성
 *
 * 파일 시스템 저장과 DB 저장은
 * 동일한 트랜잭션 자원이 아니기 때문에
 * DB Transaction이 Rollback되면
 * 저장된 이미지 파일도 별도로 삭제합니다.
 */
@Service
@RequiredArgsConstructor
public class ImageUploadService {

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

  private final ImageStorageProperties
      imageStorageProperties;

  /**
   * 로그인 사용자의 이미지를 업로드합니다.
   */
  @Transactional
  public ImageUploadResponse uploadImage(
      Long userId,
      MultipartFile file
  ) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(
                () ->
                    new CustomException(
                        ErrorCode.USER_NOT_FOUND
                    )
            );

    validateBasicFile(
        file
    );

    String contentType =
        normalizeContentType(
            file.getContentType()
        );

    String extension =
        detectAndValidateImageType(
            file,
            contentType
        );

    String originalFileName =
        normalizeOriginalFileName(
            file.getOriginalFilename(),
            extension
        );

    StoredImageFile storedImageFile;

    try {
      storedImageFile =
          localImageStorageService.store(
              file,
              extension
          );

    } catch (IOException e) {
      throw new CustomException(
          ErrorCode.IMAGE_STORAGE_FAILED
      );
    }

    /*
     * DB Transaction이 나중에 Rollback되는 경우
     * 이미 디스크에 저장된 파일이 고아 파일로
     * 남지 않도록 정리합니다.
     */
    registerRollbackCleanup(
        storedImageFile.storedFileName()
    );

    String imageUrl =
        buildImageUrl(
            storedImageFile.storedFileName()
        );

    ImageLog imageLog =
        ImageLog.createUploaded(
            user,
            imageUrl,
            originalFileName,
            storedImageFile.storedFileName(),
            contentType,
            storedImageFile.fileSize()
        );

    ImageLog saved =
        imageLogRepository.save(
            imageLog
        );

    return ImageUploadResponse.from(
        saved
    );
  }

  /**
   * 파일 존재 여부와 크기를 검증합니다.
   */
  private void validateBasicFile(
      MultipartFile file
  ) {
    if (
        file == null
            || file.isEmpty()
            || file.getSize() <= 0
    ) {
      throw new CustomException(
          ErrorCode.IMAGE_FILE_EMPTY
      );
    }

    if (
        file.getSize()
            > imageStorageProperties.getMaxFileSize()
    ) {
      throw new CustomException(
          ErrorCode.IMAGE_FILE_TOO_LARGE
      );
    }
  }

  /**
   * Content-Type을 정규화합니다.
   */
  private String normalizeContentType(
      String contentType
  ) {
    if (
        contentType == null
            || contentType.isBlank()
    ) {
      throw new CustomException(
          ErrorCode.UNSUPPORTED_IMAGE_TYPE
      );
    }

    return contentType
        .trim()
        .toLowerCase(
            Locale.ROOT
        );
  }

  /**
   * 확장자만 믿지 않고
   * 실제 파일의 시작 바이트까지 확인합니다.
   *
   * Content-Type + 파일 시그니처가
   * 서로 일치해야 정상 이미지로 인정합니다.
   */
  private String detectAndValidateImageType(
      MultipartFile file,
      String contentType
  ) {
    byte[] header;

    try (
        InputStream inputStream =
            file.getInputStream()
    ) {
      header =
          inputStream.readNBytes(8);

    } catch (IOException e) {
      throw new CustomException(
          ErrorCode.UNSUPPORTED_IMAGE_TYPE
      );
    }

    boolean jpeg =
        isJpeg(
            header
        );

    boolean png =
        isPng(
            header
        );

    if (
        JPEG_CONTENT_TYPE.equals(contentType)
            && jpeg
    ) {
      return "jpg";
    }

    if (
        PNG_CONTENT_TYPE.equals(contentType)
            && png
    ) {
      return "png";
    }

    throw new CustomException(
        ErrorCode.UNSUPPORTED_IMAGE_TYPE
    );
  }

  /**
   * JPEG 파일의 대표적인 Magic Number:
   *
   * FF D8 FF
   */
  private boolean isJpeg(
      byte[] header
  ) {
    return header.length >= 3
        && (header[0] & 0xFF) == 0xFF
        && (header[1] & 0xFF) == 0xD8
        && (header[2] & 0xFF) == 0xFF;
  }

  /**
   * PNG 파일의 Magic Number:
   *
   * 89 50 4E 47 0D 0A 1A 0A
   */
  private boolean isPng(
      byte[] header
  ) {
    return header.length >= 8
        && (header[0] & 0xFF) == 0x89
        && (header[1] & 0xFF) == 0x50
        && (header[2] & 0xFF) == 0x4E
        && (header[3] & 0xFF) == 0x47
        && (header[4] & 0xFF) == 0x0D
        && (header[5] & 0xFF) == 0x0A
        && (header[6] & 0xFF) == 0x1A
        && (header[7] & 0xFF) == 0x0A;
  }

  /**
   * 원본 파일명은 표시/추적 목적으로만 저장합니다.
   *
   * 실제 로컬 저장 파일명에는 사용하지 않습니다.
   */
  private String normalizeOriginalFileName(
      String originalFileName,
      String extension
  ) {
    if (
        originalFileName == null
            || originalFileName.isBlank()
    ) {
      return "image."
          + extension;
    }

    String normalized =
        originalFileName
            .replace(
                '\\',
                '/'
            )
            .replace(
                "\r",
                ""
            )
            .replace(
                "\n",
                ""
            )
            .trim();

    int lastSlash =
        normalized.lastIndexOf('/');

    if (lastSlash >= 0) {
      normalized =
          normalized.substring(
              lastSlash + 1
          );
    }

    if (normalized.isBlank()) {
      normalized =
          "image."
              + extension;
    }

    /*
     * ImageLog.originalFileName 컬럼 길이는
     * 최대 500자입니다.
     */
    if (normalized.length() > 500) {
      normalized =
          normalized.substring(
              0,
              500
          );
    }

    return normalized;
  }

  /**
   * 현재는 로컬 파일이지만,
   * 서버의 절대 Windows 경로를 노출하지 않고
   * HTTP API 기준 URL만 DB에 저장합니다.
   *
   * 다음 단계에서 실제 조회 API를 연결합니다.
   */
  private String buildImageUrl(
      String storedFileName
  ) {
    return "/api/images/files/"
        + storedFileName;
  }

  /**
   * 파일 시스템과 DB는 하나의 ACID Transaction으로
   * 묶을 수 없습니다.
   *
   * 따라서 DB Transaction이 Rollback되면
   * 외부 자원인 로컬 이미지 파일을 직접 삭제합니다.
   */
  private void registerRollbackCleanup(
      String storedFileName
  ) {
    if (
        !TransactionSynchronizationManager
            .isSynchronizationActive()
    ) {
      return;
    }

    TransactionSynchronizationManager
        .registerSynchronization(
            new TransactionSynchronization() {

              @Override
              public void afterCompletion(
                  int status
              ) {
                if (
                    status
                        == TransactionSynchronization
                        .STATUS_ROLLED_BACK
                ) {
                  localImageStorageService
                      .deleteIfExists(
                          storedFileName
                      );
                }
              }
            }
        );
  }
}