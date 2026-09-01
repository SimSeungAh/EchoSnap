package com.smartrecycle.backend.domain.user.service;

import com.smartrecycle.backend.domain.user.dto.admin.AdminUserDtos;
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
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserService {

  private final UserRepository userRepository;

  public PageResponse<
      AdminUserDtos.UserResponse
      >
  search(
      Long adminId,
      String keyword,
      Pageable pageable
  ) {
    validateAdmin(adminId);

    Page<User> page =
        userRepository.searchAdminUsers(
            normalizeKeyword(keyword),
            pageable
        );

    return PageResponse.from(
        page,
        AdminUserDtos.UserResponse::from
    );
  }

  public AdminUserDtos.UserResponse get(
      Long adminId,
      Long userId
  ) {
    validateAdmin(adminId);

    return AdminUserDtos.UserResponse.from(
        getUser(userId)
    );
  }

  @Transactional
  public AdminUserDtos.UserResponse updateStatus(
      Long adminId,
      Long userId,
      AdminUserDtos.UpdateStatusRequest request
  ) {
    validateAdmin(adminId);

    User target =
        getUser(userId);

    /*
     * 현재 로그인한 관리자가 자기 계정을 정지/탈퇴 상태로
     * 바꾸면 관리자 페이지에서 즉시 접근을 잃을 수 있으므로
     * 자기 자신은 ACTIVE 상태만 유지하도록 보호합니다.
     */
    if (
        adminId.equals(userId)
            && request.status()
            != UserStatus.ACTIVE
    ) {
      throw new CustomException(
          ErrorCode.INVALID_INPUT
      );
    }

    target.changeStatus(
        request.status()
    );

    return AdminUserDtos.UserResponse.from(
        target
    );
  }

  @Transactional
  public AdminUserDtos.UserResponse updateRole(
      Long adminId,
      Long userId,
      AdminUserDtos.UpdateRoleRequest request
  ) {
    validateAdmin(adminId);

    getUser(userId);

    /*
     * 현재 로그인한 관리자가 자기 ADMIN 권한을 제거하면
     * 관리 기능을 더 이상 사용할 수 없으므로 방지합니다.
     */
    if (
        adminId.equals(userId)
            && request.role()
            != Role.ADMIN
    ) {
      throw new CustomException(
          ErrorCode.INVALID_INPUT
      );
    }

    int updated =
        userRepository.updateRole(
            userId,
            request.role()
        );

    if (updated == 0) {
      throw new CustomException(
          ErrorCode.USER_NOT_FOUND
      );
    }

    /*
     * updateRole은 벌크 UPDATE이고 clearAutomatically=true라
     * 영속성 컨텍스트가 비워집니다.
     * 따라서 변경 후 최신 값을 다시 조회해서 반환합니다.
     */
    return AdminUserDtos.UserResponse.from(
        getUser(userId)
    );
  }

  private User getUser(
      Long userId
  ) {
    return userRepository
        .findById(userId)
        .orElseThrow(
            () ->
                new CustomException(
                    ErrorCode.USER_NOT_FOUND
                )
        );
  }

  private void validateAdmin(
      Long adminId
  ) {
    User admin =
        userRepository
            .findById(adminId)
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

  private String normalizeKeyword(
      String keyword
  ) {
    if (keyword == null) {
      return "";
    }

    return keyword.trim();
  }
}
