package com.smartrecycle.backend.domain.image.client;

import com.smartrecycle.backend.domain.image.dto.external.YoloAnalysisResponse;
import com.smartrecycle.backend.global.config.AiServerProperties;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/**
 * Spring Boot에서 Python FastAPI AI 서버를 호출하는
 * HTTP Client입니다.
 *
 * 이 클래스의 책임:
 *
 * - multipart 이미지 전송
 * - timeout 적용
 * - FastAPI HTTP 응답 수신
 * - 통신 실패 원인 분류
 *
 * ImageLog 상태 변경이나
 * WasteItem 매핑 같은 비즈니스 로직은
 * Service 계층에서 담당합니다.
 */
@Component
public class YoloAiClient {

  private static final String ANALYZE_PATH =
      "/analyze";

  private final RestClient restClient;

  public YoloAiClient(
      AiServerProperties properties
  ) {
    SimpleClientHttpRequestFactory requestFactory =
        new SimpleClientHttpRequestFactory();

    requestFactory.setConnectTimeout(
        properties.getConnectTimeout()
    );

    requestFactory.setReadTimeout(
        properties.getReadTimeout()
    );

    this.restClient =
        RestClient.builder()
            .baseUrl(
                properties.getBaseUrl()
            )
            .requestFactory(
                requestFactory
            )
            .build();
  }

  /**
   * 로컬 저장 이미지 파일을
   * FastAPI /analyze로 multipart 전송합니다.
   */
  public YoloAnalysisResponse analyze(
      Resource imageResource,
      String fileName,
      MediaType mediaType
  ) {
    MultipartBodyBuilder multipart =
        new MultipartBodyBuilder();

    multipart
        .part(
            "file",
            imageResource
        )
        .filename(
            fileName
        )
        .contentType(
            mediaType
        );

    try {
      YoloAnalysisResponse response =
          restClient
              .post()
              .uri(
                  ANALYZE_PATH
              )
              .contentType(
                  MediaType.MULTIPART_FORM_DATA
              )
              .body(
                  multipart.build()
              )
              .retrieve()
              .body(
                  YoloAnalysisResponse.class
              );

      if (response == null) {
        throw new AiServerClientException(
            AiServerFailureType.INVALID_RESPONSE,
            "AI 서버 응답 본문이 비어 있습니다."
        );
      }

      return response;

    } catch (AiServerClientException e) {

      /*
       * 직접 생성한 연동 예외는
       * 그대로 상위 Service로 전달합니다.
       */
      throw e;

    } catch (ResourceAccessException e) {

      /*
       * TCP 연결 실패 또는
       * 응답 timeout 등의 네트워크 오류입니다.
       */
      if (isTimeoutException(e)) {
        throw new AiServerClientException(
            AiServerFailureType.TIMEOUT,
            "AI 서버 응답 시간이 초과되었습니다.",
            e
        );
      }

      if (isConnectionException(e)) {
        throw new AiServerClientException(
            AiServerFailureType.CONNECTION_FAILED,
            "AI 서버에 연결할 수 없습니다.",
            e
        );
      }

      throw new AiServerClientException(
          AiServerFailureType.CONNECTION_FAILED,
          "AI 서버 통신에 실패했습니다.",
          e
      );

    } catch (RestClientResponseException e) {

      /*
       * FastAPI가 응답은 했지만
       * HTTP 상태가 4xx 또는 5xx인 경우입니다.
       */
      HttpStatusCode statusCode =
          e.getStatusCode();

      int status =
          statusCode.value();

      if (status == 503) {
        throw new AiServerClientException(
            AiServerFailureType.SERVICE_UNAVAILABLE,
            status,
            "AI 분석 서비스를 현재 사용할 수 없습니다.",
            e
        );
      }

      if (statusCode.is5xxServerError()) {
        throw new AiServerClientException(
            AiServerFailureType.SERVER_ERROR,
            status,
            "AI 서버에서 오류가 발생했습니다.",
            e
        );
      }

      if (statusCode.is4xxClientError()) {
        throw new AiServerClientException(
            AiServerFailureType.CLIENT_ERROR,
            status,
            "AI 서버가 분석 요청을 거부했습니다.",
            e
        );
      }

      throw new AiServerClientException(
          AiServerFailureType.INVALID_RESPONSE,
          status,
          "AI 서버에서 예상하지 못한 HTTP 응답을 반환했습니다.",
          e
      );

    } catch (RestClientException e) {

      /*
       * JSON 역직렬화 실패 등
       * 기타 RestClient 처리 오류입니다.
       */
      throw new AiServerClientException(
          AiServerFailureType.INVALID_RESPONSE,
          "AI 서버 응답을 처리할 수 없습니다.",
          e
      );
    }
  }

  /**
   * cause 체인을 확인하여
   * timeout인지 판단합니다.
   *
   * SimpleClientHttpRequestFactory 기반에서는
   * 연결/읽기 timeout이 일반적으로
   * SocketTimeoutException으로 전달됩니다.
   */
  private boolean isTimeoutException(
      Throwable throwable
  ) {
    Throwable current =
        throwable;

    while (current != null) {

      if (
          current
              instanceof SocketTimeoutException
      ) {
        return true;
      }

      current =
          current.getCause();
    }

    return false;
  }

  /**
   * cause 체인을 확인하여
   * 서버 연결 실패인지 판단합니다.
   */
  private boolean isConnectionException(
      Throwable throwable
  ) {
    Throwable current =
        throwable;

    while (current != null) {

      if (
          current
              instanceof ConnectException
              || current
              instanceof UnknownHostException
              || current
              instanceof NoRouteToHostException
      ) {
        return true;
      }

      current =
          current.getCause();
    }

    return false;
  }
}