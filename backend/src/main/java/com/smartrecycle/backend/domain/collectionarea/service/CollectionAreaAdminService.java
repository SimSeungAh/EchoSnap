package com.smartrecycle.backend.domain.collectionarea.service;

import com.smartrecycle.backend.domain.collectionarea.dto.response.CollectionAreaSyncResultResponse;
import com.smartrecycle.backend.domain.user.entity.Role;
import com.smartrecycle.backend.domain.user.entity.User;
import com.smartrecycle.backend.domain.user.repository.UserRepository;
import com.smartrecycle.backend.global.exception.CustomException;
import com.smartrecycle.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CollectionAreaAdminService {

  private final UserRepository userRepository;

  private final CollectionAreaPublicDataSyncService
      collectionAreaPublicDataSyncService;

  /**
   * 관리자가 행정안전부 생활쓰레기배출정보를
   * SmartRecycle DB에 동기화합니다.
   *
   * CollectionArea와 CollectionAreaSchedule이
   * 공공데이터 최신 내용으로 생성/갱신됩니다.
   */
  public CollectionAreaSyncResultResponse
  syncPublicData(
      Long adminId
  ) {
    getAdmin(
        adminId
    );

    return collectionAreaPublicDataSyncService
        .syncAll();
  }

  /**
   * 사용자 ID를 조회하고
   * 실제 ADMIN 권한인지 다시 확인합니다.
   *
   * SecurityConfig에서도 /api/admin/**를
   * ADMIN으로 제한하지만,
   * Service에서도 권한을 다시 검증해
   * 도메인 로직을 직접 호출하는 경우까지 보호합니다.
   */
  private User getAdmin(
      Long adminId
  ) {
    User user =
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
        user.getRole()
            != Role.ADMIN
    ) {
      throw new CustomException(
          ErrorCode.FORBIDDEN
      );
    }

    return user;
  }
}