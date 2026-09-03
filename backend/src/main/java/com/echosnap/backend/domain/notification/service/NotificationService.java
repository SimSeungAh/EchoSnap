package com.echosnap.backend.domain.notification.service;

import com.echosnap.backend.domain.notification.entity.Notification;
import com.echosnap.backend.domain.notification.repository.NotificationRepository;
import com.echosnap.backend.domain.user.entity.ResidenceType;
import com.echosnap.backend.domain.user.entity.Role;
import com.echosnap.backend.domain.user.entity.User;
import com.echosnap.backend.domain.user.entity.UserStatus;
import com.echosnap.backend.domain.user.repository.UserRepository;
import com.echosnap.backend.global.exception.CustomException;
import com.echosnap.backend.global.exception.ErrorCode;
import com.echosnap.backend.global.response.PageResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

  private final NotificationRepository
      notificationRepository;

  private final UserRepository
      userRepository;

  /*
   * =========================================================
   * 관리자
   * =========================================================
   */

  /**
   * 관리자 알림 발송
   */
  @Transactional
  public AdminNotificationResponse send(
      Long adminId,
      CreateNotificationRequest request
  ) {
    User admin =
        getAdmin(
            adminId
        );

    Notification notification =
        Notification.send(
            request.title().trim(),
            request.body().trim(),
            request.targetType(),
            admin
        );

    Notification saved =
        notificationRepository.save(
            notification
        );

    return AdminNotificationResponse.from(
        saved
    );
  }

  /**
   * 관리자 발송 이력
   */
  public PageResponse<AdminNotificationResponse>
  getAdminNotifications(
      Long adminId,
      String keyword,
      Notification.Status status,
      Notification.TargetType targetType,
      Pageable pageable
  ) {
    getAdmin(
        adminId
    );

    Page<Notification> page =
        notificationRepository.searchAdmin(
            normalizeKeyword(
                keyword
            ),
            status,
            targetType,
            pageable
        );

    return PageResponse.from(
        page,
        AdminNotificationResponse::from
    );
  }

  /**
   * 발송 취소
   */
  @Transactional
  public AdminNotificationResponse cancel(
      Long adminId,
      Long notificationId
  ) {
    getAdmin(
        adminId
    );

    Notification notification =
        getNotification(
            notificationId
        );

    notification.cancel();

    return AdminNotificationResponse.from(
        notification
    );
  }

  /*
   * =========================================================
   * 사용자
   * =========================================================
   */

  /**
   * 사용자 알림함
   */
  public PageResponse<UserNotificationResponse>
  getUserNotifications(
      Long userId,
      Pageable pageable
  ) {
    User user =
        getActiveUser(
            userId
        );

    Page<Notification> page =
        findVisiblePage(
            user,
            pageable
        );

    return PageResponse.from(
        page,
        notification ->
            UserNotificationResponse.from(
                notification,
                userId
            )
    );
  }

  /**
   * 읽지 않은 알림 수
   *
   * Flutter 알림 배지에 사용합니다.
   */
  public UnreadCountResponse
  getUnreadCount(
      Long userId
  ) {
    User user =
        getActiveUser(
            userId
        );

    long count =
        notificationRepository
            .countUnreadForUser(
                userId,
                user.isNotificationEnabled(),
                user.getResidenceType(),

                Notification.Status.SENT,

                Notification.TargetType
                    .ALL_ACTIVE_USERS,

                Notification.TargetType
                    .NOTIFICATION_ENABLED_USERS,

                Notification.TargetType
                    .MANAGED_COMPLEX_USERS,

                Notification.TargetType
                    .GENERAL_HOUSING_USERS,

                ResidenceType.MANAGED_COMPLEX,
                ResidenceType.GENERAL_HOUSING
            );

    return new UnreadCountResponse(
        count
    );
  }

  /**
   * 알림 하나 읽음
   */
  @Transactional
  public UserNotificationResponse
  markAsRead(
      Long userId,
      Long notificationId
  ) {
    User user =
        getActiveUser(
            userId
        );

    Notification notification =
        getNotification(
            notificationId
        );

    if (
        !isVisibleToUser(
            notification,
            user
        )
    ) {
      throw new CustomException(
          ErrorCode.FORBIDDEN
      );
    }

    notification.markAsRead(
        userId
    );

    return UserNotificationResponse.from(
        notification,
        userId
    );
  }

  /**
   * 사용자에게 현재 보이는 알림 전체 읽음
   */
  @Transactional
  public void markAllAsRead(
      Long userId
  ) {
    User user =
        getActiveUser(
            userId
        );

    var notifications =
        notificationRepository
            .findAllVisibleForUser(
                user.isNotificationEnabled(),
                user.getResidenceType(),

                Notification.Status.SENT,

                Notification.TargetType
                    .ALL_ACTIVE_USERS,

                Notification.TargetType
                    .NOTIFICATION_ENABLED_USERS,

                Notification.TargetType
                    .MANAGED_COMPLEX_USERS,

                Notification.TargetType
                    .GENERAL_HOUSING_USERS,

                ResidenceType.MANAGED_COMPLEX,
                ResidenceType.GENERAL_HOUSING
            );

    for (
        Notification notification
        : notifications
    ) {
      notification.markAsRead(
          userId
      );
    }
  }

  /*
   * =========================================================
   * 공통
   * =========================================================
   */

  private Page<Notification> findVisiblePage(
      User user,
      Pageable pageable
  ) {
    return notificationRepository
        .findVisibleForUser(
            user.isNotificationEnabled(),
            user.getResidenceType(),

            Notification.Status.SENT,

            Notification.TargetType
                .ALL_ACTIVE_USERS,

            Notification.TargetType
                .NOTIFICATION_ENABLED_USERS,

            Notification.TargetType
                .MANAGED_COMPLEX_USERS,

            Notification.TargetType
                .GENERAL_HOUSING_USERS,

            ResidenceType.MANAGED_COMPLEX,
            ResidenceType.GENERAL_HOUSING,

            pageable
        );
  }

  private boolean isVisibleToUser(
      Notification notification,
      User user
  ) {
    if (
        notification.getStatus()
            != Notification.Status.SENT
    ) {
      return false;
    }

    return switch (
        notification.getTargetType()
        ) {
      case ALL_ACTIVE_USERS ->
          true;

      case NOTIFICATION_ENABLED_USERS ->
          user.isNotificationEnabled();

      case MANAGED_COMPLEX_USERS ->
          user.getResidenceType()
              == ResidenceType.MANAGED_COMPLEX;

      case GENERAL_HOUSING_USERS ->
          user.getResidenceType()
              == ResidenceType.GENERAL_HOUSING;
    };
  }

  private Notification getNotification(
      Long notificationId
  ) {
    return notificationRepository
        .findById(
            notificationId
        )
        .orElseThrow(
            () ->
                new CustomException(
                    ErrorCode.INVALID_INPUT
                )
        );
  }

  private User getAdmin(
      Long adminId
  ) {
    User admin =
        getActiveUser(
            adminId
        );

    if (
        admin.getRole()
            != Role.ADMIN
    ) {
      throw new CustomException(
          ErrorCode.FORBIDDEN
      );
    }

    return admin;
  }

  private User getActiveUser(
      Long userId
  ) {
    User user =
        userRepository
            .findById(
                userId
            )
            .orElseThrow(
                () ->
                    new CustomException(
                        ErrorCode.USER_NOT_FOUND
                    )
            );

    if (
        user.getStatus()
            != UserStatus.ACTIVE
    ) {
      throw new CustomException(
          ErrorCode.FORBIDDEN
      );
    }

    return user;
  }

  private String normalizeKeyword(
      String keyword
  ) {
    if (keyword == null) {
      return "";
    }

    return keyword.trim();
  }

  /*
   * =========================================================
   * DTO
   * =========================================================
   */

  public record CreateNotificationRequest(

      @NotBlank
      @Size(max = 120)
      String title,

      @NotBlank
      @Size(max = 2000)
      String body,

      @NotNull
      Notification.TargetType targetType
  ) {
  }

  public record AdminNotificationResponse(
      Long id,
      String title,
      String body,
      Notification.TargetType targetType,
      Notification.Status status,
      String senderEmail,
      LocalDateTime sentAt,
      LocalDateTime cancelledAt,
      LocalDateTime createdAt
  ) {

    public static AdminNotificationResponse from(
        Notification notification
    ) {
      return new AdminNotificationResponse(
          notification.getId(),
          notification.getTitle(),
          notification.getBody(),
          notification.getTargetType(),
          notification.getStatus(),

          notification.getSender()
              .getEmail(),

          notification.getSentAt(),
          notification.getCancelledAt(),
          notification.getCreatedAt()
      );
    }
  }

  public record UserNotificationResponse(
      Long id,
      String title,
      String body,
      Notification.TargetType targetType,
      LocalDateTime sentAt,
      boolean read
  ) {

    public static UserNotificationResponse from(
        Notification notification,
        Long userId
    ) {
      return new UserNotificationResponse(
          notification.getId(),
          notification.getTitle(),
          notification.getBody(),
          notification.getTargetType(),
          notification.getSentAt(),
          notification.isReadBy(
              userId
          )
      );
    }
  }

  public record UnreadCountResponse(
      long unreadCount
  ) {
  }
}