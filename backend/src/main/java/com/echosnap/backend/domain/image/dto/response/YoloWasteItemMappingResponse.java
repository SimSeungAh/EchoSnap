package com.echosnap.backend.domain.image.dto.response;

import com.echosnap.backend.domain.waste.entity.WasteItem;

/**
 * YOLO label을 EchoSnap WasteItem으로
 * 변환한 결과를 표현합니다.
 *
 * 다음 실제 서버 재분석 Service에서 사용합니다.
 */
public record YoloWasteItemMappingResponse(

    String label,

    boolean mapped,

    Long wasteItemId,

    String wasteItemName

) {

  public static YoloWasteItemMappingResponse mapped(
      String label,
      WasteItem wasteItem
  ) {
    return new YoloWasteItemMappingResponse(
        label,
        true,
        wasteItem.getId(),
        wasteItem.getName()
    );
  }

  public static YoloWasteItemMappingResponse unmapped(
      String label
  ) {
    return new YoloWasteItemMappingResponse(
        label,
        false,
        null,
        null
    );
  }
}