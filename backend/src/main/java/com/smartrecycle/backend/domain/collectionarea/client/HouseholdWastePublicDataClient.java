package com.smartrecycle.backend.domain.collectionarea.client;

import com.smartrecycle.backend.domain.collectionarea.dto.external.HouseholdWastePublicDataResponse;
import com.smartrecycle.backend.global.config.HouseholdWastePublicDataProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class HouseholdWastePublicDataClient {

  private static final Logger log =
      LoggerFactory.getLogger(
          HouseholdWastePublicDataClient.class
      );

  private static final String RETURN_TYPE_JSON =
      "json";

  private final RestClient restClient;

  private final HouseholdWastePublicDataProperties
      properties;

  public HouseholdWastePublicDataClient(
      HouseholdWastePublicDataProperties properties
  ) {
    this.properties = properties;

    /*
     * 실제 URI는 fetchPage()에서 직접 구성합니다.
     *
     * 공공데이터포털 serviceKey가 이미
     * URL Encoding 된 형태로 발급되는 경우,
     * UriBuilder.queryParam()을 거치면서
     * % 문자가 다시 인코딩되는 문제를
     * 방지하기 위함입니다.
     */
    this.restClient =
        RestClient.builder()
            .build();
  }

  /**
   * 행정안전부 생활쓰레기배출정보를
   * 페이지 단위로 조회합니다.
   */
  public HouseholdWastePublicDataResponse fetchPage(
      int pageNo,
      int numOfRows
  ) {
    URI requestUri =
        buildRequestUri(
            pageNo,
            numOfRows
        );

    try {
      return restClient
          .get()
          .uri(requestUri)
          .retrieve()
          .body(
              HouseholdWastePublicDataResponse.class
          );

    } catch (RestClientResponseException e) {

      /*
       * serviceKey 자체는 절대 로그에 출력하지 않습니다.
       *
       * 외부 API의 HTTP 상태 코드와
       * 응답 본문 일부만 출력해서
       * 인증 오류 / 주소 오류 / 요청 파라미터 오류를
       * 구분할 수 있도록 합니다.
       */
      log.error(
          "생활쓰레기 공공데이터 API 호출 실패. "
              + "status={}, body={}",
          e.getStatusCode(),
          abbreviate(
              e.getResponseBodyAsString(),
              500
          )
      );

      throw e;
    }
  }

  /**
   * 공공데이터포털 요청 URI를 직접 만듭니다.
   *
   * 이미 URL Encoding 되어 있는 serviceKey는
   * 그대로 사용하고,
   *
   * Encoding 되어 있지 않은 키라면
   * 한 번만 Encoding 합니다.
   */
  private URI buildRequestUri(
      int pageNo,
      int numOfRows
  ) {
    String requestUrl =
        normalizeRequestUrl(
            properties.getRequestUrl()
        );

    String serviceKey =
        prepareServiceKey(
            properties.getServiceKey()
        );

    String separator =
        requestUrl.contains("?")
            ? "&"
            : "?";

    String requestUri =
        requestUrl
            + separator
            + "serviceKey="
            + serviceKey
            + "&pageNo="
            + pageNo
            + "&numOfRows="
            + numOfRows
            + "&returnType="
            + RETURN_TYPE_JSON;

    return URI.create(
        requestUri
    );
  }

  /**
   * serviceKey가 이미 %XX 형태로
   * Encoding 되어 있으면 그대로 사용합니다.
   *
   * 그렇지 않은 경우에만 한 번 Encoding 합니다.
   */
  private String prepareServiceKey(
      String serviceKey
  ) {
    if (
        serviceKey == null
            || serviceKey.isBlank()
    ) {
      return "";
    }

    String trimmed =
        serviceKey.trim();

    /*
     * 공공데이터포털에서 전달된
     * URL Encoding 키의 일반적인 형태입니다.
     *
     * 예를 들어 %2F, %3D 등이 포함된 경우
     * 이미 Encoding 된 것으로 판단합니다.
     */
    if (trimmed.contains("%")) {
      return trimmed;
    }

    return URLEncoder.encode(
        trimmed,
        StandardCharsets.UTF_8
    );
  }

  /**
   * application-secret.yml에서
   * /info를 이미 포함했다면 그대로 사용합니다.
   *
   * 실수로 Base URL까지만 입력한 경우에도
   * 안전하게 /info를 보완합니다.
   */
  private String normalizeRequestUrl(
      String requestUrl
  ) {
    if (
        requestUrl == null
            || requestUrl.isBlank()
    ) {
      throw new IllegalStateException(
          "생활쓰레기 공공데이터 API URL이 "
              + "설정되지 않았습니다."
      );
    }

    String normalized =
        requestUrl.trim();

    while (normalized.endsWith("/")) {
      normalized =
          normalized.substring(
              0,
              normalized.length() - 1
          );
    }

    if (
        normalized.endsWith(
            "/info"
        )
    ) {
      return normalized;
    }

    return normalized + "/info";
  }

  /**
   * 외부 API 오류 응답이 지나치게 길 경우
   * 로그가 과도하게 커지는 것을 막습니다.
   */
  private String abbreviate(
      String value,
      int maxLength
  ) {
    if (value == null) {
      return "";
    }

    if (value.length() <= maxLength) {
      return value;
    }

    return value.substring(
        0,
        maxLength
    ) + "...";
  }
}