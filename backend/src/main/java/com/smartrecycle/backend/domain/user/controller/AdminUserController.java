package com.smartrecycle.backend.domain.user.controller;

import com.smartrecycle.backend.domain.user.dto.admin.AdminUserDtos;
import com.smartrecycle.backend.domain.user.service.AdminUserService;
import com.smartrecycle.backend.global.response.ApiResponse;
import com.smartrecycle.backend.global.response.PageResponse;
import com.smartrecycle.backend.global.security.service.CustomUserDetails;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
    "/api/admin/users"
)
@RequiredArgsConstructor
@Tag(
    name = "Admin User",
    description = "관리자 사용자 계정 관리 API"
)
public class AdminUserController {

  private final AdminUserService
      adminUserService;

  @GetMapping
  @Operation(
      summary = "관리자 사용자 목록 조회",
      description = """
          이메일, 닉네임, 공동주택명,
          공동주택 주소, 일반주택 주소로 검색합니다.
          """
  )
  public ApiResponse<
      PageResponse<
          AdminUserDtos.UserResponse
          >
      >
  search(
      @AuthenticationPrincipal
      CustomUserDetails userDetails,

      @RequestParam(
          defaultValue = ""
      )
      String keyword,

      @ParameterObject
      @PageableDefault(
          size = 20,
          sort = "createdAt",
          direction = Sort.Direction.DESC
      )
      Pageable pageable
  ) {
    return ApiResponse.success(
        "관리자 사용자 목록 조회 성공",
        adminUserService.search(
            userDetails.getUserId(),
            keyword,
            pageable
        )
    );
  }

  @GetMapping(
      "/{userId}"
  )
  @Operation(
      summary = "관리자 사용자 상세 조회"
  )
  public ApiResponse<
      AdminUserDtos.UserResponse
      >
  get(
      @AuthenticationPrincipal
      CustomUserDetails userDetails,

      @PathVariable
      Long userId
  ) {
    return ApiResponse.success(
        "관리자 사용자 상세 조회 성공",
        adminUserService.get(
            userDetails.getUserId(),
            userId
        )
    );
  }

  @PatchMapping(
      "/{userId}/status"
  )
  @Operation(
      summary = "관리자 사용자 상태 변경"
  )
  public ApiResponse<
      AdminUserDtos.UserResponse
      >
  updateStatus(
      @AuthenticationPrincipal
      CustomUserDetails userDetails,

      @PathVariable
      Long userId,

      @Valid
      @RequestBody
      AdminUserDtos.UpdateStatusRequest request
  ) {
    return ApiResponse.success(
        "사용자 상태 변경 성공",
        adminUserService.updateStatus(
            userDetails.getUserId(),
            userId,
            request
        )
    );
  }

  @PatchMapping(
      "/{userId}/role"
  )
  @Operation(
      summary = "관리자 사용자 권한 변경"
  )
  public ApiResponse<
      AdminUserDtos.UserResponse
      >
  updateRole(
      @AuthenticationPrincipal
      CustomUserDetails userDetails,

      @PathVariable
      Long userId,

      @Valid
      @RequestBody
      AdminUserDtos.UpdateRoleRequest request
  ) {
    return ApiResponse.success(
        "사용자 권한 변경 성공",
        adminUserService.updateRole(
            userDetails.getUserId(),
            userId,
            request
        )
    );
  }
}
