package com.echosnap.backend.global.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.echosnap.backend.global.exception.CustomException;
import com.echosnap.backend.global.exception.ErrorCode;
import com.echosnap.backend.global.response.ApiResponse;
import com.echosnap.backend.global.security.jwt.JwtProvider;
import com.echosnap.backend.global.security.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtProvider jwtProvider;
  private final CustomUserDetailsService customUserDetailsService;

  /*
   * 현재 프로젝트의 JwtAuthenticationEntryPoint,
   * JwtAccessDeniedHandler와 동일한 방식으로
   * ObjectMapper를 직접 생성합니다.
   *
   * 따라서 Spring Bean 등록에 의존하지 않습니다.
   */
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {

    String token = resolveToken(request);

    if (token == null) {
      filterChain.doFilter(
          request,
          response
      );

      return;
    }

    try {
      /*
       * JWT의 서명, 만료 여부, 형식을 검증합니다.
       */
      jwtProvider.validateToken(token);

      String email =
          jwtProvider.getEmail(token);

      UserDetails userDetails =
          customUserDetailsService
              .loadUserByUsername(email);

      UsernamePasswordAuthenticationToken authentication =
          new UsernamePasswordAuthenticationToken(
              userDetails,
              null,
              userDetails.getAuthorities()
          );

      SecurityContextHolder
          .getContext()
          .setAuthentication(authentication);

      filterChain.doFilter(
          request,
          response
      );

    } catch (CustomException exception) {

      /*
       * JWT가 만료되거나 잘못된 경우
       * Controller까지 예외를 넘기지 않고
       * 여기서 직접 401 JSON 응답으로 변환합니다.
       */
      SecurityContextHolder.clearContext();

      writeAuthenticationError(
          response,
          exception.getErrorCode()
      );
    }
  }

  private void writeAuthenticationError(
      HttpServletResponse response,
      ErrorCode errorCode
  ) throws IOException {

    response.setStatus(
        errorCode
            .getStatus()
            .value()
    );

    response.setContentType(
        "application/json"
    );

    response.setCharacterEncoding(
        "UTF-8"
    );

    ApiResponse<Void> apiResponse =
        ApiResponse.fail(
            errorCode.getCode(),
            errorCode.getMessage()
        );

    objectMapper.writeValue(
        response.getWriter(),
        apiResponse
    );
  }

  private String resolveToken(
      HttpServletRequest request
  ) {
    String bearerToken =
        request.getHeader(
            "Authorization"
        );

    if (
        bearerToken != null
            && bearerToken.startsWith(
            "Bearer "
        )
    ) {
      return bearerToken.substring(7);
    }

    return null;
  }
}