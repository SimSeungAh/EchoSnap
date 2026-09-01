package com.smartrecycle.backend.domain.collectionarea.dto.admin;

import com.smartrecycle.backend.domain.collectionarea.entity.CollectionArea;
import com.smartrecycle.backend.domain.collectionarea.entity.CollectionAreaSourceType;
import com.smartrecycle.backend.domain.collectionarea.entity.CollectionWasteType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public final class AdminCollectionAreaDtos {

  private AdminCollectionAreaDtos() {
  }

  /**
   * CollectionArea 실제 원본 데이터 응답.
   *
   * 상세 조회 / 등록 / 수정 / 활성화 응답과
   * 지역 그룹 상세의 원본 목록에서 사용합니다.
   */
  public record CollectionAreaResponse(
      Long id,
      CollectionAreaSourceType sourceType,
      String externalManagementNumber,

      String sido,
      String sigungu,

      String areaName,
      String targetAreaName,

      List<CollectionWasteType> supportedWasteTypes,

      LocalDate sourceReferenceDate,

      boolean active,

      LocalDateTime createdAt,
      LocalDateTime updatedAt
  ) {

    public static CollectionAreaResponse from(
        CollectionArea area
    ) {
      List<CollectionWasteType> wasteTypes =
          area.getSupportedWasteTypes()
              .stream()
              .sorted(
                  Comparator.comparing(
                      Enum::name
                  )
              )
              .toList();

      return new CollectionAreaResponse(
          area.getId(),
          area.getSourceType(),
          area.getExternalManagementNumber(),

          area.getSido(),
          area.getSigungu(),

          area.getAreaName(),
          area.getTargetAreaName(),

          wasteTypes,

          area.getSourceReferenceDate(),

          area.isActive(),

          area.getCreatedAt(),
          area.getUpdatedAt()
      );
    }
  }

  /**
   * 관리자 수거구역 목록용 지역 그룹.
   *
   * targetAreaName이 존재하면 targetAreaName을,
   * 비어 있으면 areaName을 실제 표시 대상지역으로 사용합니다.
   */
  public record CollectionAreaGroupResponse(
      String sido,
      String sigungu,

      String targetAreaName,

      CollectionAreaSourceType sourceType,

      boolean active,

      long originalCount
  ) {
  }

  /**
   * 지역 그룹 상세.
   *
   * 관리자 목록에서는 지역 한 줄만 보여주고,
   * 상세에서는 해당 그룹에 포함된 실제 CollectionArea 원본을
   * 모두 확인할 수 있습니다.
   */
  public record CollectionAreaGroupDetailResponse(
      String sido,
      String sigungu,

      String targetAreaName,

      CollectionAreaSourceType sourceType,

      boolean active,

      long originalCount,

      List<CollectionAreaResponse> originals
  ) {
  }

  /**
   * 관리자가 직접 수거구역 등록
   */
  public record CreateRequest(

      @NotBlank
      @Size(max = 50)
      String sido,

      @NotBlank
      @Size(max = 100)
      String sigungu,

      @NotBlank
      @Size(max = 200)
      String areaName,

      @Size(max = 500)
      String targetAreaName,

      @NotEmpty
      Set<CollectionWasteType> supportedWasteTypes
  ) {
  }

  /**
   * MANUAL 수거구역 수정
   */
  public record UpdateRequest(

      @NotBlank
      @Size(max = 50)
      String sido,

      @NotBlank
      @Size(max = 100)
      String sigungu,

      @NotBlank
      @Size(max = 200)
      String areaName,

      @Size(max = 500)
      String targetAreaName,

      @NotEmpty
      Set<CollectionWasteType> supportedWasteTypes,

      @NotNull
      Boolean active
  ) {
  }
}