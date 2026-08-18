package com.smartrecycle.backend.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 행정안전부 생활쓰레기배출정보
 * 공공데이터 API 연동 설정입니다.
 *
 * 실제 요청 URL과 공공데이터포털 인증키는
 * application-secret.yml에서 주입합니다.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(
    prefix = "public-data.household-waste"
)
public class HouseholdWastePublicDataProperties {

  /**
   * 공공데이터포털 활용신청 후 제공되는
   * 생활쓰레기배출정보 API 요청 주소입니다.
   */
  private String requestUrl;

  /**
   * 공공데이터포털에서 발급한 인증키입니다.
   */
  private String serviceKey;
}