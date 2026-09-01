package com.smartrecycle.backend.domain.collectionarea.service;

import com.smartrecycle.backend.domain.collectionarea.dto.admin.AdminCollectionAreaDtos;
import com.smartrecycle.backend.domain.collectionarea.dto.response.CollectionAreaSyncResultResponse;
import com.smartrecycle.backend.domain.collectionarea.entity.CollectionArea;
import com.smartrecycle.backend.domain.collectionarea.entity.CollectionAreaSourceType;
import com.smartrecycle.backend.domain.collectionarea.repository.CollectionAreaRepository;
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
public class CollectionAreaAdminService {

  private final UserRepository userRepository;

  private final CollectionAreaRepository
      collectionAreaRepository;

  private final CollectionAreaPublicDataSyncService
      collectionAreaPublicDataSyncService;

  /**
   * 관리자 수거구역 목록
   */
  public PageResponse<
      AdminCollectionAreaDtos.CollectionAreaResponse
      >
  search(
      Long adminId,
      String keyword,
      CollectionAreaSourceType sourceType,
      Boolean active,
      Pageable pageable
  ) {
    getAdmin(adminId);

    Page<CollectionArea> page =
        collectionAreaRepository
            .searchAdminAreas(
                normalizeKeyword(keyword),
                sourceType,
                active,
                pageable
            );

    return PageResponse.from(
        page,
        AdminCollectionAreaDtos
            .CollectionAreaResponse
            ::from
    );
  }

  /**
   * 관리자 수거구역 상세
   */
  public AdminCollectionAreaDtos.CollectionAreaResponse
  get(
      Long adminId,
      Long collectionAreaId
  ) {
    getAdmin(adminId);

    return AdminCollectionAreaDtos
        .CollectionAreaResponse
        .from(
            getCollectionArea(
                collectionAreaId
            )
        );
  }

  /**
   * 관리자가 수거구역 직접 등록
   *
   * 직접 등록한 데이터는
   * sourceType = MANUAL 입니다.
   */
  @Transactional
  public AdminCollectionAreaDtos.CollectionAreaResponse
  create(
      Long adminId,
      AdminCollectionAreaDtos.CreateRequest request
  ) {
    getAdmin(adminId);

    CollectionArea area =
        CollectionArea.createManual(
            request.sido().trim(),
            request.sigungu().trim(),
            request.areaName().trim(),
            trimToNull(
                request.targetAreaName()
            ),
            request.supportedWasteTypes()
        );

    CollectionArea saved =
        collectionAreaRepository.save(
            area
        );

    return AdminCollectionAreaDtos
        .CollectionAreaResponse
        .from(saved);
  }

  /**
   * 관리자가 직접 만든 수거구역 수정
   *
   * 공공데이터 수거구역은 다음 동기화 때
   * 원본 데이터로 다시 덮일 수 있으므로
   * 일반 수정 API에서 변경하지 않습니다.
   */
  @Transactional
  public AdminCollectionAreaDtos.CollectionAreaResponse
  update(
      Long adminId,
      Long collectionAreaId,
      AdminCollectionAreaDtos.UpdateRequest request
  ) {
    getAdmin(adminId);

    CollectionArea area =
        getCollectionArea(
            collectionAreaId
        );

    if (
        area.getSourceType()
            != CollectionAreaSourceType.MANUAL
    ) {
      throw new CustomException(
          ErrorCode.INVALID_INPUT
      );
    }

    area.updateManual(
        request.sido().trim(),
        request.sigungu().trim(),
        request.areaName().trim(),
        trimToNull(
            request.targetAreaName()
        ),
        request.supportedWasteTypes()
    );

    if (request.active()) {
      area.activate();
    } else {
      area.deactivate();
    }

    return AdminCollectionAreaDtos
        .CollectionAreaResponse
        .from(area);
  }

  /**
   * 공공데이터 여부와 관계없이
   * 수거구역 사용 중지
   *
   * 삭제하지 않고 active=false 처리합니다.
   */
  @Transactional
  public AdminCollectionAreaDtos.CollectionAreaResponse
  deactivate(
      Long adminId,
      Long collectionAreaId
  ) {
    getAdmin(adminId);

    CollectionArea area =
        getCollectionArea(
            collectionAreaId
        );

    area.deactivate();

    return AdminCollectionAreaDtos
        .CollectionAreaResponse
        .from(area);
  }

  /**
   * 비활성 수거구역 재활성화
   */
  @Transactional
  public AdminCollectionAreaDtos.CollectionAreaResponse
  activate(
      Long adminId,
      Long collectionAreaId
  ) {
    getAdmin(adminId);

    CollectionArea area =
        getCollectionArea(
            collectionAreaId
        );

    area.activate();

    return AdminCollectionAreaDtos
        .CollectionAreaResponse
        .from(area);
  }

  /**
   * 행정안전부 생활쓰레기배출정보 전체 데이터를
   * SmartRecycle DB와 동기화합니다.
   */
  @Transactional
  public CollectionAreaSyncResultResponse
  syncPublicData(
      Long adminId
  ) {
    getAdmin(adminId);

    return collectionAreaPublicDataSyncService
        .syncAll();
  }

  private CollectionArea getCollectionArea(
      Long collectionAreaId
  ) {
    return collectionAreaRepository
        .findById(
            collectionAreaId
        )
        .orElseThrow(
            () ->
                new CustomException(
                    ErrorCode.INVALID_INPUT
                )
        );
  }

  /**
   * 실제 활성 관리자만 허용
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
            || user.getStatus()
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

  private String trimToNull(
      String value
  ) {
    if (
        value == null
            || value.isBlank()
    ) {
      return null;
    }

    return value.trim();
  }
}