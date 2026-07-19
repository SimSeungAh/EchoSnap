package com.smartrecycle.backend.global.config;

import com.smartrecycle.backend.global.security.filter.JwtAuthenticationFilter;
import com.smartrecycle.backend.global.security.handler.JwtAccessDeniedHandler;
import com.smartrecycle.backend.global.security.handler.JwtAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private static final String[] WHITE_LIST = {
          "/api/auth/**",
          "/swagger-ui/**",
          "/swagger-ui.html",
          "/v3/api-docs/**"
  };

  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final JwtAuthenticationEntryPoint authenticationEntryPoint;
  private final JwtAccessDeniedHandler accessDeniedHandler;

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public SecurityFilterChain filterChain(
          HttpSecurity http
  ) throws Exception {

    http
            .cors(cors ->
                    cors.configurationSource(
                            corsConfigurationSource()
                    )
            )
            .csrf(csrf -> csrf.disable())
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .sessionManagement(session ->
                    session.sessionCreationPolicy(
                            SessionCreationPolicy.STATELESS
                    )
            )
            .exceptionHandling(exception ->
                    exception
                            .authenticationEntryPoint(
                                    authenticationEntryPoint
                            )
                            .accessDeniedHandler(
                                    accessDeniedHandler
                            )
            )
            .authorizeHttpRequests(auth ->
                    auth
                            .requestMatchers(WHITE_LIST)
                            .permitAll()

                            /*
                             * 관리자 API는 ADMIN 권한만 접근할 수 있습니다.
                             */
                            .requestMatchers("/api/admin/**")
                            .hasRole("ADMIN")

                            /*
                             * 나머지 API는 로그인한 사용자만 접근할 수 있습니다.
                             */
                            .anyRequest()
                            .authenticated()
            )
            .addFilterBefore(
                    jwtAuthenticationFilter,
                    UsernamePasswordAuthenticationFilter.class
            );

    return http.build();
  }

  @Bean
  public CorsConfigurationSource
  corsConfigurationSource() {

    CorsConfiguration configuration =
            new CorsConfiguration();

    configuration.setAllowedOrigins(
            List.of("http://localhost:5173")
    );

    configuration.setAllowedMethods(
            List.of(
                    "GET",
                    "POST",
                    "PUT",
                    "PATCH",
                    "DELETE",
                    "OPTIONS"
            )
    );

    configuration.setAllowedHeaders(
            List.of("*")
    );

    configuration.setExposedHeaders(
            List.of("Authorization")
    );

    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source =
            new UrlBasedCorsConfigurationSource();

    source.registerCorsConfiguration(
            "/**",
            configuration
    );

    return source;
  }
}