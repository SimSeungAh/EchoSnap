package com.smartrecycle.backend.domain.image.service;

import com.smartrecycle.backend.domain.image.dto.response.ImageFileResponse;
import com.smartrecycle.backend.domain.image.entity.ImageLog;
import com.smartrecycle.backend.domain.image.repository.ImageLogRepository;
import com.smartrecycle.backend.domain.image.storage.LocalImageStorageService;
import com.smartrecycle.backend.global.exception.CustomException;
import com.smartrecycle.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

/**
 * 저장된 사용자 이미지를 조회하는 Service입니다.
 *
 * 실제 파일명을 알고 있더라도
 * 해당 이미지의 소유 사용자만 접근할 수 있도록
 * ImageLog의 userId를 함께 검증합니다.
 */
@Service
@RequiredArgsConstructor
public class ImageFileService {

  private static final String JPEG_CONTENT_TYPE =
      "image/jpeg";

  private static final String PNG_CONTENT_TYPE =
      "image/png";

  private final ImageLogRepository
      imageLogRepository;

  private final LocalImageStorageService
      localImageStorageService;

  /**
   * 로그인한 사용자가
   * 자신의 이미지 파일을 조회합니다.
   */
  @Transactional(readOnly = true)
  public ImageFileResponse getMyImageFile(
      Long userId,
      String storedFileName
  ) {
    /*
     * storedFileName만 조회하지 않고
     * 반드시 userId를 함께 사용합니다.
     *
     * 따라서 다른 사용자의 UUID 파일명을
     * 우연히 알게 되더라도 접근할 수 없습니다.
     */
    ImageLog imageLog =
        imageLogRepository
            .findByStoredFileNameAndUserId(
                storedFileName,
                userId
            )
            .orElseThrow(
                () ->
                    new CustomException(
                        ErrorCode.IMAGE_LOG_NOT_FOUND
                    )
            );

    Resource resource;

    try {
      resource =
          localImageStorageService
              .loadAsResource(
                  imageLog.getStoredFileName()
              );

    } catch (IOException e) {

      /*
       * DB에는 ImageLog가 존재하지만
       * 실제 파일이 삭제되었거나 읽을 수 없는 경우입니다.
       *
       * 이는 사용자 입력 오류가 아니라
       * 서버 저장소와 DB의 불일치이므로
       * IMAGE_STORAGE_FAILED로 처리합니다.
       */
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
   * ImageLog에 저장된 Content-Type을
   * 실제 HTTP MediaType으로 변환합니다.
   *
   * 이미지 업로드 시 이미 JPEG/PNG만 허용했기 때문에
   * 여기에서도 허용된 두 형식만 반환합니다.
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

    /*
     * DB에 허용되지 않은 Content-Type이 들어 있다면
     * 정상적인 업로드 데이터가 아니므로
     * 서버 저장소 오류로 처리합니다.
     */
    throw new CustomException(
        ErrorCode.IMAGE_STORAGE_FAILED
    );
  }
}