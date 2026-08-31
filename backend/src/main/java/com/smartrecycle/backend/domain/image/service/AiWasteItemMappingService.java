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
   * Python YOLO 서버 모델 계열 식별자입니다.
   */
  public static final String
      YOLO_MODEL_NAME =
      "SMARTRECYCLE_YOLO";

  /**
   * Flutter TensorFlow Lite 모델 계열 식별자입니다.
   *
   * 서버 YOLO와 모바일 TFLite가
   * 같은 label 문자열을 사용하더라도
   * 모델 계열을 분리해 관리합니다.
   */
  public static final String
      TFLITE_MODEL_NAME =
      "SMARTRECYCLE_TFLITE";

  private final AiWasteItemMappingRepository
      aiWasteItemMappingRepository;

  /**
   * YOLO가 반환한 label에 해당하는
   * 활성 WasteItem을 조회합니다.
   */
  @Transactional(readOnly = true)
  public Optional<WasteItem>
  findWasteItemByYoloLabel(
      String label
  ) {
    return findWasteItemByModelLabel(
        YOLO_MODEL_NAME,
        label
    );
  }

  /**
   * Flutter TFLite가 반환한 label에 해당하는
   * 활성 WasteItem을 조회합니다.
   */
  @Transactional(readOnly = true)
  public Optional<WasteItem>
  findWasteItemByTfliteLabel(
      String label
  ) {
    return findWasteItemByModelLabel(
        TFLITE_MODEL_NAME,
        label
    );
  }

  /**
   * 공통 AI label 매핑 조회 로직입니다.
   *
   * 매핑이 없을 경우 문자열 이름으로
   * 임의의 WasteItem을 추측하지 않고
   * Optional.empty()를 반환합니다.
   */
  private Optional<WasteItem>
  findWasteItemByModelLabel(
      String modelName,
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
            modelName,
            label.trim()
        )
        .map(
            AiWasteItemMapping::getWasteItem
        )
        .filter(
            WasteItem::isActive
        );
  }
}