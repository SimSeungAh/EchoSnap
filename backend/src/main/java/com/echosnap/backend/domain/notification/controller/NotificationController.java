package com.echosnap.backend.domain.notification.controller;

import com.echosnap.backend.domain.notification.entity.Notification;
import com.echosnap.backend.domain.notification.service.NotificationService;
import com.echosnap.backend.global.response.ApiResponse;
import com.echosnap.backend.global.response.PageResponse;
import com.echosnap.backend.global.security.service.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(
    name = "Notification",
    description = "사용자 알림 및 관리자 알림 발송 API"
)
public class NotificationController {

  private final NotificationService
      notificationService;

  /*
   * =========================================================
   * 사용자
   * =========================================================
   */

  @GetMapping("/api/notifications")
  @Operation(
      summary = "내 알림 목록 조회"
  )
  public ApiResponse<
      PageResponse<
          NotificationService
              .UserNotificationResponse
          >
      >
  getMyNotifications(
      @AuthenticationPrincipal
      CustomUserDetails userDetails,

      @ParameterObject
      @PageableDefault(
          size = 20,
          sort = "sentAt",
          direction = Sort.Direction.DESC
      )
      Pageable pageable
  ) {
    return ApiResponse.success(
        "알림 목록 조회 성공",
        notificationService
            .getUserNotifications(
                userDetails.getUserId(),
                pageable
            )
    );
  }

  @GetMapping(
      "/api/notifications/unread-count"
  )
  @Operation(
      summary = "읽지 않은 알림 개수 조회"
  )
  public ApiResponse<
      NotificationService.UnreadCountResponse
      >
  getUnreadCount(
      @AuthenticationPrincipal
      CustomUserDetails userDetails
  ) {
    return ApiResponse.success(
        "읽지 않은 알림 개수 조회 성공",
        notificationService
            .getUnreadCount(
                userDetails.getUserId()
            )
    );
  }

  @PatchMapping(
      "/api/notifications/{notificationId}/read"
  )
  @Operation(
      summary = "알림 읽음 처리"
  )
  public ApiResponse<
      NotificationService
          .UserNotificationResponse
      >
  markAsRead(
      @AuthenticationPrincipal
      CustomUserDetails userDetails,

      @PathVariable
      Long notificationId
  ) {
    return ApiResponse.success(
        "알림을 읽음 처리했습니다.",
        notificationService.markAsRead(
            userDetails.getUserId(),
            notificationId
        )
    );
  }

  @PatchMapping(
      "/api/notifications/read-all"
  )
  @Operation(
      summary = "전체 알림 읽음 처리"
  )
  public ApiResponse<Void>
  markAllAsRead(
      @AuthenticationPrincipal
      CustomUserDetails userDetails
  ) {
    notificationService.markAllAsRead(
        userDetails.getUserId()
    );

    return ApiResponse.success(
        "모든 알림을 읽음 처리했습니다."
    );
  }

  /*
   * =========================================================
   * 관리자
   * =========================================================
   */

  @GetMapping(
      "/api/admin/notifications"
  )
  @Operation(
      summary = "관리자 알림 발송 이력 조회"
  )
  public ApiResponse<
      PageResponse<
          NotificationService
              .AdminNotificationResponse
          >
      >
  getAdminNotifications(
      @AuthenticationPrincipal
      CustomUserDetails userDetails,

      @RequestParam(
          defaultValue = ""
      )
      String keyword,

      @RequestParam(
          required = false
      )
      Notification.Status status,

      @RequestParam(
          required = false
      )
      Notification.TargetType targetType,

      @ParameterObject
      @PageableDefault(
          size = 20,
          sort = "sentAt",
          direction = Sort.Direction.DESC
      )
      Pageable pageable
  ) {
    return ApiResponse.success(
        "관리자 알림 발송 이력 조회 성공",
        notificationService
            .getAdminNotifications(
                userDetails.getUserId(),
                keyword,
                status,
                targetType,
                pageable
            )
    );
  }

  @PostMapping(
      "/api/admin/notifications"
  )
  @Operation(
      summary = "관리자 알림 발송",
      description = """
                    targetType:

                    ALL_ACTIVE_USERS
                    - 활성 사용자 전체

                    NOTIFICATION_ENABLED_USERS
                    - 알림 수신 동의 사용자

                    MANAGED_COMPLEX_USERS
                    - 아파트·오피스텔 등 공동주택 사용자

                    GENERAL_HOUSING_USERS
                    - 일반주택 사용자
                    """
  )
  public ApiResponse<
      NotificationService
          .AdminNotificationResponse
      >
  send(
      @AuthenticationPrincipal
      CustomUserDetails userDetails,

      @Valid
      @RequestBody
      NotificationService
          .CreateNotificationRequest request
  ) {
    return ApiResponse.success(
        "알림이 발송되었습니다.",
        notificationService.send(
            userDetails.getUserId(),
            request
        )
    );
  }

  @PatchMapping(
      "/api/admin/notifications/{notificationId}/cancel"
  )
  @Operation(
      summary = "관리자 알림 발송 취소",
      description = """
                    발송 이력은 보존하지만
                    사용자 알림함에서는 더 이상 노출되지 않습니다.
                    """
  )
  public ApiResponse<
      NotificationService
          .AdminNotificationResponse
      >
  cancel(
      @AuthenticationPrincipal
      CustomUserDetails userDetails,

      @PathVariable
      Long notificationId
  ) {
    return ApiResponse.success(
        "알림 발송이 취소되었습니다.",
        notificationService.cancel(
            userDetails.getUserId(),
            notificationId
        )
    );
  }
}