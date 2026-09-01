package com.smartrecycle.backend.domain.publicdata.service;

import com.smartrecycle.backend.domain.collectionarea.dto.response.CollectionAreaSyncResultResponse;
import com.smartrecycle.backend.domain.collectionarea.service.CollectionAreaPublicDataSyncService;
import com.smartrecycle.backend.domain.publicdata.entity.PublicDataSyncLog;
import com.smartrecycle.backend.domain.publicdata.repository.PublicDataSyncLogRepository;
import com.smartrecycle.backend.domain.user.entity.Role;
import com.smartrecycle.backend.domain.user.entity.User;
import com.smartrecycle.backend.domain.user.entity.UserStatus;
import com.smartrecycle.backend.domain.user.repository.UserRepository;
import com.smartrecycle.backend.global.exception.CustomException;
import com.smartrecycle.backend.global.exception.ErrorCode;
import com.smartrecycle.backend.global.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminPublicDataService {

  private static final String HOUSEHOLD_WASTE_SOURCE =
      "행정안전부 생활쓰레기배출정보";

  private final UserRepository
      userRepository;

  private final PublicDataSyncLogRepository
      publicDataSyncLogRepository;

  private final CollectionAreaPublicDataSyncService
      collectionAreaPublicDataSyncService;

  /**
   * 관리자 공공데이터 동기화
   *
   * 1. RUNNING 이력 저장
   * 2. 실제 공공데이터 동기화
   * 3. SUCCESS 또는 FAILED 저장
   */
  public SyncExecutionResponse sync(
      Long adminId
  ) {
    validateAdmin(
        adminId
    );

    PublicDataSyncLog log =
        PublicDataSyncLog.start(
            HOUSEHOLD_WASTE_SOURCE
        );

    publicDataSyncLogRepository.saveAndFlush(
        log
    );

    try {

      CollectionAreaSyncResultResponse result =
          collectionAreaPublicDataSyncService
              .syncAll();

      log.completeSuccess(
          result.createdCount(),
          result.updatedCount(),
          result.skippedCount()
      );

      publicDataSyncLogRepository.saveAndFlush(
          log
      );

      return new SyncExecutionResponse(
          SyncLogResponse.from(
              log
          ),
          result
      );

    } catch (RuntimeException exception) {

      log.completeFailure(
          exception.getMessage()
      );

      publicDataSyncLogRepository.saveAndFlush(
          log
      );

      throw exception;
    }
  }

  /**
   * 관리자 동기화 이력
   */
  public PageResponse<SyncLogResponse>
  getLogs(
      Long adminId,
      Pageable pageable
  ) {
    validateAdmin(
        adminId
    );

    Page<PublicDataSyncLog> page =
        publicDataSyncLogRepository
            .findAllByOrderByStartedAtDesc(
                pageable
            );

    return PageResponse.from(
        page,
        SyncLogResponse::from
    );
  }

  private void validateAdmin(
      Long adminId
  ) {
    User admin =
        userRepository
            .findById(
                adminId
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

  /**
   * React 관리자 공공데이터 화면에서
   * 그대로 사용할 수 있는 형태입니다.
   */
  public record SyncLogResponse(
      Long id,
      String source,
      String status,
      LocalDateTime startedAt,
      LocalDateTime finishedAt,
      int insertedCount,
      int updatedCount,
      int failedCount,
      int skippedCount,
      String message
  ) {

    public static SyncLogResponse from(
        PublicDataSyncLog log
    ) {
      return new SyncLogResponse(
          log.getId(),
          log.getSource(),
          log.getStatus()
              .name(),
          log.getStartedAt(),
          log.getFinishedAt(),
          log.getInsertedCount(),
          log.getUpdatedCount(),
          log.getFailedCount(),
          log.getSkippedCount(),
          log.getMessage()
      );
    }
  }

  /**
   * 수동 동기화 실행 직후에는
   * 이력 + 실제 동기화 결과를 함께 반환합니다.
   */
  public record SyncExecutionResponse(
      SyncLogResponse log,
      CollectionAreaSyncResultResponse result
  ) {
  }
}