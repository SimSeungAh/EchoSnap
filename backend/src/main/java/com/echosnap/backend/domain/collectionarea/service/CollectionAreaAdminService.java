package com.echosnap.backend.domain.collectionarea.service;

import com.echosnap.backend.domain.collectionarea.dto.admin.AdminCollectionAreaDtos;
import com.echosnap.backend.domain.collectionarea.dto.response.CollectionAreaSyncResultResponse;
import com.echosnap.backend.domain.collectionarea.entity.CollectionArea;
import com.echosnap.backend.domain.collectionarea.entity.CollectionAreaSourceType;
import com.echosnap.backend.domain.collectionarea.repository.CollectionAreaRepository;
import com.echosnap.backend.domain.user.entity.Role;
import com.echosnap.backend.domain.user.entity.User;
import com.echosnap.backend.domain.user.entity.UserStatus;
import com.echosnap.backend.domain.user.repository.UserRepository;
import com.echosnap.backend.global.exception.CustomException;
import com.echosnap.backend.global.exception.ErrorCode;
import com.echosnap.backend.global.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
   * 관리자 수거구역 지역 그룹 목록.
   *
   * CollectionArea 원본 단위가 아니라
   * 관리자 화면에 실제 표시되는 지역 그룹 단위로
   * 페이지네이션합니다.
   */
  public PageResponse<
      AdminCollectionAreaDtos.CollectionAreaGroupResponse
      >
  search(
      Long adminId,
      String keyword,
      CollectionAreaSourceType sourceType,
      Boolean active,
      Pageable pageable
  ) {
    getAdmin(adminId);

    /*
     * Repository의 그룹 조회 쿼리 자체에서
     * 지역 기준 정렬을 수행합니다.
     *
     * 기존 Controller의 updatedAt 정렬이나
     * 클라이언트 임의 정렬이 native query에 추가되지 않도록
     * 페이지 번호와 크기만 사용합니다.
     */
    Pageable groupPageable =
        PageRequest.of(
            pageable.getPageNumber(),
            pageable.getPageSize()
        );

    Page<
        CollectionAreaRepository
            .AdminCollectionAreaGroupProjection
        > page =
        collectionAreaRepository
            .searchAdminCollectionAreaGroups(
                normalizeKeyword(keyword),
                sourceType == null
                    ? null
                    : sourceType.name(),
                active,
                groupPageable
            );

    return PageResponse.from(
        page,
        projection ->
            new AdminCollectionAreaDtos
                .CollectionAreaGroupResponse(
                projection.getSido(),
                projection.getSigungu(),
                projection.getTargetAreaName(),
                CollectionAreaSourceType.valueOf(
                    projection.getSourceTypeName()
                ),
                Boolean.TRUE.equals(
                    projection.getActive()
                ),
                projection.getOriginalCount() == null
                    ? 0L
                    : projection.getOriginalCount()
            )
    );
  }

  /**
   * 관리자 수거구역 지역 그룹 상세.
   *
   * 목록에서 한 줄로 묶인 지역에 포함된
   * 실제 CollectionArea 원본을 모두 반환합니다.
   */
  public AdminCollectionAreaDtos
      .CollectionAreaGroupDetailResponse
  getGroup(
      Long adminId,
      String sido,
      String sigungu,
      String targetAreaName,
      CollectionAreaSourceType sourceType,
      boolean active
  ) {
    getAdmin(adminId);

    String normalizedSido =
        requireText(sido);

    String normalizedSigungu =
        requireText(sigungu);

    String normalizedTargetAreaName =
        requireText(targetAreaName);

    if (sourceType == null) {
      throw new CustomException(
          ErrorCode.INVALID_INPUT
      );
    }

    List<CollectionArea> originals =
        collectionAreaRepository
            .findAllByAdminCollectionAreaGroup(
                normalizedSido,
                normalizedSigungu,
                normalizedTargetAreaName,
                sourceType.name(),
                active
            );

    if (originals.isEmpty()) {
      throw new CustomException(
          ErrorCode.INVALID_INPUT
      );
    }

    List<
        AdminCollectionAreaDtos.CollectionAreaResponse
        > originalResponses =
        originals.stream()
            .map(
                AdminCollectionAreaDtos
                    .CollectionAreaResponse
                    ::from
            )
            .toList();

    return new AdminCollectionAreaDtos
        .CollectionAreaGroupDetailResponse(
        normalizedSido,
        normalizedSigungu,
        normalizedTargetAreaName,
        sourceType,
        active,
        originalResponses.size(),
        originalResponses
    );
  }

  /**
   * 관리자 수거구역 실제 원본 상세.
   *
   * 그룹 상세에서 특정 CollectionArea 원본 하나를
   * 다시 확인할 때 기존 API를 그대로 사용할 수 있습니다.
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
   * 관리자가 수거구역 직접 등록.
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
   * 관리자가 직접 만든 수거구역 수정.
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
   * 수거구역 사용 중지.
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
   * 비활성 수거구역 재활성화.
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
   * EchoSnap DB와 동기화합니다.
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
   * 실제 활성 관리자만 허용.
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

  /**
   * 그룹 식별에 반드시 필요한 문자열을 검증합니다.
   */
  private String requireText(
      String value
  ) {
    if (
        value == null
            || value.isBlank()
    ) {
      throw new CustomException(
          ErrorCode.INVALID_INPUT
      );
    }

    return value.trim();
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