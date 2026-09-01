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