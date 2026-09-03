package com.echosnap.backend.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "juso")
public class JusoProperties {
  private String baseUrl = "https://business.juso.go.kr";
  private String confirmationKey;

  public boolean isConfigured() {
    return confirmationKey != null
        && !confirmationKey.isBlank()
        && !confirmationKey.startsWith("your-");
  }
}
