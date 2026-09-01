package com.smartrecycle.backend.domain.admin.service;

import com.smartrecycle.backend.domain.admin.dto.AdminDashboardDtos;
import com.smartrecycle.backend.domain.apartment.entity.ApartmentStatus;
import com.smartrecycle.backend.domain.apartment.repository.ApartmentRepository;
import com.smartrecycle.backend.domain.image.entity.ImageLog;
import com.smartrecycle.backend.domain.image.entity.ImageReviewStatus;
import com.smartrecycle.backend.domain.image.repository.ImageLogRepository;
import com.smartrecycle.backend.domain.notification.entity.Notification;
import com.smartrecycle.backend.domain.notification.repository.NotificationRepository;
import com.smartrecycle.backend.domain.publicdata.entity.PublicDataSyncLog;
import com.smartrecycle.backend.domain.publicdata.repository.PublicDataSyncLogRepository;
import com.smartrecycle.backend.domain.user.entity.Role;
import com.smartrecycle.backend.domain.user.entity.User;
import com.smartrecycle.backend.domain.user.entity.UserStatus;
import com.smartrecycle.backend.domain.user.repository.UserRepository;
import com.smartrecycle.backend.domain.waste.repository.WasteItemRepository;
import com.smartrecycle.backend.global.exception.CustomException;
import com.smartrecycle.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardService {

  private static final int RECENT_CORRECTION_SIZE = 5;

  /**
   * SmartRecycle은 국내 생활폐기물 서비스를 기준으로 하므로
   * 관리자 대시보드의 "오늘"도 한국 시간을 기준으로 계산합니다.
   */
  private static final ZoneId SERVICE_ZONE_ID =
      ZoneId.of("Asia/Seoul");

  private final UserRepository
      userRepository;

  private final ApartmentRepository
      apartmentRepository;

  private final WasteItemRepository
      wasteItemRepository;

  private final ImageLogRepository
      imageLogRepository;

  private final PublicDataSyncLogRepository
      publicDataSyncLogRepository;

  private final NotificationRepository
      notificationRepository;

  /**
   * 관리자 메인 대시보드 조회
   */
  public AdminDashboardDtos.DashboardResponse
  getDashboard(
      Long adminUserId
  ) {
    validateAdmin(
        adminUserId
    );

    /*
     * 전체 사용자
     */
    long totalUsers =
        userRepository.count();

    /*
     * 활성 사용자
     */
    long activeUsers =
        userRepository.countByStatus(
            UserStatus.ACTIVE
        );

    /*
     * 관리자 승인을 기다리는 공동주택
     */
    long pendingResidences =
        apartmentRepository
            .searchByStatusAndKeyword(
                ApartmentStatus.PENDING,
                "",
                PageRequest.of(
                    0,
                    1
                )
            )
            .getTotalElements();

    /*
     * 등록된 전체 폐기물 품목
     */
    long wasteItems =
        wasteItemRepository.count();

    /*
     * AI 사용자 정정 검수 대기
     */
    Page<ImageLog> pendingCorrectionPage =
        imageLogRepository
            .findAllByReviewStatusOrderByCreatedAtAsc(
                ImageReviewStatus.PENDING,
                PageRequest.of(
                    0,
                    RECENT_CORRECTION_SIZE
                )
            );

    long pendingAiCorrections =
        pendingCorrectionPage
            .getTotalElements();

    /*
     * 최근 AI 정정 5건
     */
    List<
        AdminDashboardDtos
            .RecentCorrectionResponse
        > recentCorrections =
        pendingCorrectionPage
            .getContent()
            .stream()
            .map(
                AdminDashboardDtos
                    .RecentCorrectionResponse
                    ::from
            )
            .toList();

    /*
     * 최근 공공데이터 동기화 5건
     */
    List<PublicDataSyncLog> syncLogs =
        publicDataSyncLogRepository
            .findTop5ByOrderByStartedAtDesc();

    List<
        AdminDashboardDtos
            .RecentSyncResponse
        > recentSyncLogs =
        syncLogs
            .stream()
            .map(
                AdminDashboardDtos
                    .RecentSyncResponse
                    ::from
            )
            .toList();

    /*
     * 한국 시간 기준 오늘 00:00 ~ 내일 00:00
     */
    LocalDate today =
        LocalDate.now(
            SERVICE_ZONE_ID
        );

    LocalDateTime todayStart =
        today.atStartOfDay();

    LocalDateTime tomorrowStart =
        today
            .plusDays(1)
            .atStartOfDay();

    /*
     * 오늘 실제 발송된 알림 수
     *
     * CANCELLED 알림은 현재 유효한 발송 건수에서
     * 제외하고 SENT 상태만 집계합니다.
     */
    long todayNotifications =
        notificationRepository
            .countByStatusAndSentAtGreaterThanEqualAndSentAtLessThan(
                Notification.Status.SENT,
                todayStart,
                tomorrowStart
            );

    return new AdminDashboardDtos.DashboardResponse(
        totalUsers,
        activeUsers,
        pendingResidences,
        wasteItems,
        pendingAiCorrections,
        todayNotifications,
        recentCorrections,
        recentSyncLogs
    );
  }

  private void validateAdmin(
      Long userId
  ) {
    User admin =
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
        admin.getRole()
            != Role.ADMIN
            || admin.getStatus()
            != UserStatus.ACTIVE
    ) {
      throw new CustomException(
          ErrorCode.FORBIDDEN
      );
    }
  }
}