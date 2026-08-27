package com.smartrecycle.backend.domain.image.service;

import com.smartrecycle.backend.domain.image.entity.AiWasteItemMapping;
import com.smartrecycle.backend.domain.image.repository.AiWasteItemMappingRepository;
import com.smartrecycle.backend.domain.waste.entity.WasteItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * AI 모델 label을
 * SmartRecycle WasteItem으로 변환하는 Service입니다.
 *
 * AI 모델의 classId나 label 처리 규칙을
 * 이미지 분석 Service에 직접 넣지 않고
 * 별도의 매핑 계층으로 분리합니다.
 */
@Service
@RequiredArgsConstructor
public class AiWasteItemMappingService {

  /**
   * 현재 Python YOLO 서버에서 사용하는
   * 모델 계열 식별자입니다.
   *
   * modelVersion과는 다릅니다.
   *
   * modelName:
   * 매핑 체계 식별
   *
   * modelVersion:
   * 실제 추론에 사용된 모델 버전 기록
   */
  public static final String
      YOLO_MODEL_NAME =
      "SMARTRECYCLE_YOLO";

  private final AiWasteItemMappingRepository
      aiWasteItemMappingRepository;

  /**
   * YOLO가 반환한 label에 해당하는
   * 활성 WasteItem을 조회합니다.
   *
   * 매핑이 없을 경우 Optional.empty()를 반환합니다.
   *
   * AI가 새로운 label을 반환했다고 해서
   * 문자열을 이용해 임의의 WasteItem을 추측하지 않습니다.
   */
  @Transactional(readOnly = true)
  public Optional<WasteItem>
  findWasteItemByYoloLabel(
      String label
  ) {
    if (
        label == null
            || label.isBlank()
    ) {
      return Optional.empty();
    }

    return aiWasteItemMappingRepository
        .findByModelNameIgnoreCaseAndModelLabelIgnoreCaseAndActiveTrue(
            YOLO_MODEL_NAME,
            label.trim()
        )
        .map(
            AiWasteItemMapping::getWasteItem
        )
        /*
         * 관리자에 의해 숨겨진 WasteItem을
         * 새로운 AI 분석 결과로 제공하지 않습니다.
         */
        .filter(
            WasteItem::isActive
        );
  }
}