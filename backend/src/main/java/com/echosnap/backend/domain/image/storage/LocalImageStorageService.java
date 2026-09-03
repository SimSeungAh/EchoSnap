package com.echosnap.backend.domain.image.storage;

import com.echosnap.backend.global.config.ImageStorageProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

/**
 * 개발 단계에서 사용하는
 * 로컬 이미지 파일 저장 Service입니다.
 *
 * 이 클래스는 파일 시스템 처리만 담당합니다.
 *
 * 이미지 확장자 검증,
 * Content-Type 검증,
 * 사용자 조회,
 * ImageLog DB 저장 같은 비즈니스 로직은
 * 다음 단계의 ImageUploadService에서 처리합니다.
 */
@Service
@RequiredArgsConstructor
public class LocalImageStorageService {

  private final ImageStorageProperties
      imageStorageProperties;

  private Path rootDirectory;

  /**
   * Spring Boot 시작 시
   * 이미지 저장 폴더를 준비합니다.
   */
  @PostConstruct
  public void initialize() {
    try {
      rootDirectory =
          Path.of(
                  imageStorageProperties
                      .getRootPath()
              )
              .toAbsolutePath()
              .normalize();

      Files.createDirectories(
          rootDirectory
      );

    } catch (IOException e) {
      throw new IllegalStateException(
          "이미지 저장 디렉터리를 생성할 수 없습니다.",
          e
      );
    }
  }

  /**
   * MultipartFile을 로컬 디스크에 저장합니다.
   *
   * 사용자가 보낸 원본 파일명은
   * 실제 저장 파일명으로 사용하지 않습니다.
   *
   * UUID 기반 파일명을 새로 생성하여
   * 파일명 충돌과 경로 조작 위험을 줄입니다.
   */
  public StoredImageFile store(
      MultipartFile file,
      String extension
  ) throws IOException {

    String normalizedExtension =
        normalizeExtension(
            extension
        );

    String storedFileName =
        UUID.randomUUID()
            + "."
            + normalizedExtension;

    Path targetPath =
        resolveSafePath(
            storedFileName
        );

    try (
        InputStream inputStream =
            file.getInputStream()
    ) {
      Files.copy(
          inputStream,
          targetPath,
          StandardCopyOption.REPLACE_EXISTING
      );
    }

    long actualFileSize =
        Files.size(
            targetPath
        );

    return new StoredImageFile(
        storedFileName,
        actualFileSize
    );
  }

  /**
   * 저장된 이미지 파일을 Resource로 읽습니다.
   *
   * 다음 단계에서
   * 인증된 사용자에게 자신의 이미지를
   * 반환하는 API를 만들 때 사용합니다.
   */
  public Resource loadAsResource(
      String storedFileName
  ) throws IOException {

    Path filePath =
        resolveSafePath(
            storedFileName
        );

    if (
        !Files.exists(filePath)
            || !Files.isRegularFile(filePath)
            || !Files.isReadable(filePath)
    ) {
      throw new IOException(
          "저장된 이미지 파일을 찾을 수 없습니다."
      );
    }

    try {
      Resource resource =
          new UrlResource(
              filePath.toUri()
          );

      if (!resource.exists()) {
        throw new IOException(
            "저장된 이미지 파일을 찾을 수 없습니다."
        );
      }

      return resource;

    } catch (MalformedURLException e) {
      throw new IOException(
          "이미지 파일 경로를 읽을 수 없습니다.",
          e
      );
    }
  }

  /**
   * 저장된 파일을 삭제합니다.
   *
   * 예를 들어:
   *
   * 파일 저장 성공
   * ↓
   * ImageLog DB 저장 실패
   *
   * 상황에서 이미 저장된 파일을
   * 정리하기 위해 사용할 수 있습니다.
   */
  public void deleteIfExists(
      String storedFileName
  ) {
    if (
        storedFileName == null
            || storedFileName.isBlank()
    ) {
      return;
    }

    try {
      Path filePath =
          resolveSafePath(
              storedFileName
          );

      Files.deleteIfExists(
          filePath
      );

    } catch (IOException ignored) {

      /*
       * 정리 작업 실패 때문에
       * 원래 발생한 비즈니스 오류까지
       * 덮어쓰지 않도록 합니다.
       *
       * 운영 단계에서는 Logger를 추가하여
       * 삭제 실패를 별도로 기록할 예정입니다.
       */
    }
  }

  /**
   * 실제 저장소 절대 경로를 기준으로
   * 안전한 파일 경로를 생성합니다.
   *
   * "../" 등을 이용해 저장소 밖으로
   * 접근하는 Path Traversal을 방지합니다.
   */
  private Path resolveSafePath(
      String storedFileName
  ) throws IOException {

    if (
        storedFileName == null
            || storedFileName.isBlank()
    ) {
      throw new IOException(
          "파일명이 올바르지 않습니다."
      );
    }

    Path resolvedPath =
        rootDirectory
            .resolve(
                storedFileName
            )
            .normalize();

    if (
        !resolvedPath.startsWith(
            rootDirectory
        )
    ) {
      throw new IOException(
          "허용되지 않은 이미지 파일 경로입니다."
      );
    }

    return resolvedPath;
  }

  /**
   * 확장자를 안전한 형태로 변환합니다.
   *
   * 실제로 jpg / png인지 검증하는 작업은
   * 다음 ImageUploadService에서 합니다.
   */
  private String normalizeExtension(
      String extension
  ) {

    if (
        extension == null
            || extension.isBlank()
    ) {
      throw new IllegalArgumentException(
          "이미지 확장자가 필요합니다."
      );
    }

    String normalized =
        extension
            .trim()
            .toLowerCase(
                Locale.ROOT
            );

    if (
        normalized.startsWith(".")
    ) {
      normalized =
          normalized.substring(1);
    }

    /*
     * jpg, jpeg, png 같은
     * 단순 확장자 형태만 허용합니다.
     */
    if (
        !normalized.matches(
            "[a-z0-9]{1,10}"
        )
    ) {
      throw new IllegalArgumentException(
          "이미지 확장자가 올바르지 않습니다."
      );
    }

    return normalized;
  }
}