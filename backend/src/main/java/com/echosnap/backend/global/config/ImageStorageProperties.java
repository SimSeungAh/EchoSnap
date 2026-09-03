package com.echosnap.backend.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 이미지 로컬 저장소 설정입니다.
 *
 * 개발 초기에는 로컬 디스크를 사용하고,
 * 이후 S3 저장소로 교체할 예정입니다.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(
    prefix = "image.storage"
)
public class ImageStorageProperties {

  /**
   * 이미지가 실제로 저장될 디렉터리입니다.
   *
   * Spring Boot 실행 위치를 기준으로
   * ./uploads/images에 저장합니다.
   */
  private String rootPath =
      "./uploads/images";

  /**
   * 허용하는 이미지 최대 크기입니다.
   *
   * 기본값:
   * 10MB
   */
  private long maxFileSize =
      10L * 1024L * 1024L;
}