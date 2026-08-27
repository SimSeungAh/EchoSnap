package com.smartrecycle.backend.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * SmartRecycle Python AI 서버 연동 설정입니다.
 *
 * 실제 환경에 따라 baseUrl과 timeout을
 * application.yml 또는 환경변수로 변경할 수 있습니다.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(
    prefix = "ai.server"
)
public class AiServerProperties {

  /**
   * FastAPI 서버 주소
   *
   * 기본:
   * http://127.0.0.1:8000
   */
  private String baseUrl =
      "http://127.0.0.1:8000";

  /**
   * FastAPI 서버와 TCP 연결을 맺을 때
   * 기다리는 최대 시간
   */
  private Duration connectTimeout =
      Duration.ofSeconds(3);

  /**
   * 연결 후 YOLO 분석 응답을
   * 기다리는 최대 시간
   *
   * AI 추론은 일반 API보다 오래 걸릴 수 있어
   * 연결 timeout보다 길게 설정합니다.
   */
  private Duration readTimeout =
      Duration.ofSeconds(30);
}