package com.echosnap.backend.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "kakao.local")
public class KakaoLocalProperties {

  private String baseUrl = "https://dapi.kakao.com";

  private String restApiKey;
}