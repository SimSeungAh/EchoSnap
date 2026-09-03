package com.echosnap.backend.global.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * local 개발환경에서
 * AI 모델 label과 EchoSnap WasteItem의
 * 기본 매핑을 생성합니다.
 *
 * YOLO와 Flutter TFLite를 서로 다른
 * modelName으로 분리하여 관리합니다.
 */
@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class LocalAiMappingSeedInitializer
    implements ApplicationListener<ApplicationReadyEvent> {

  private static final String
      YOLO_MODEL_NAME =
      "ECHOSNAP_YOLO";

  private static final String
      TFLITE_MODEL_NAME =
      "ECHOSNAP_TFLITE";

  private final JdbcTemplate jdbcTemplate;

  @Override
  public void onApplicationEvent(
      ApplicationReadyEvent event
  ) {
    seedYoloMappings();
    seedTfliteMappings();
  }

  /**
   * 현재 서버 YOLO의 기존 label과
   * 최신 학습 데이터 label을 모두 허용합니다.
   *
   * 기존 개발 데이터와 새 모델이 섞여 있어도
   * label 변경 때문에 분석이 깨지지 않도록
   * 호환 alias를 함께 등록합니다.
   */
  private void seedYoloMappings() {
    upsertMapping(
        YOLO_MODEL_NAME,
        "cardboard",
        "종이박스"
    );

    upsertMapping(
        YOLO_MODEL_NAME,
        "cardboard_box",
        "종이박스"
    );

    upsertMapping(
        YOLO_MODEL_NAME,
        "pet",
        "페트병"
    );

    upsertMapping(
        YOLO_MODEL_NAME,
        "pet_bottle",
        "페트병"
    );

    upsertMapping(
        YOLO_MODEL_NAME,
        "plastic_container",
        "플라스틱 용기"
    );

    upsertMapping(
        YOLO_MODEL_NAME,
        "can",
        "캔"
    );

    upsertMapping(
        YOLO_MODEL_NAME,
        "glass",
        "유리병"
    );

    upsertMapping(
        YOLO_MODEL_NAME,
        "glass_bottle",
        "유리병"
    );

    upsertMapping(
        YOLO_MODEL_NAME,
        "styro",
        "스티로폼"
    );

    upsertMapping(
        YOLO_MODEL_NAME,
        "styrofoam",
        "스티로폼"
    );

    log.info(
        "Local YOLO WasteItem mapping seed completed."
    );
  }

  /**
   * Flutter TFLite에서 사용할
   * 6개 표준 label 매핑입니다.
   *
   * labels.txt의 순서/문자열도
   * 아래 값과 동일하게 맞춥니다.
   */
  private void seedTfliteMappings() {
    upsertMapping(
        TFLITE_MODEL_NAME,
        "cardboard_box",
        "종이박스"
    );

    upsertMapping(
        TFLITE_MODEL_NAME,
        "pet_bottle",
        "페트병"
    );

    upsertMapping(
        TFLITE_MODEL_NAME,
        "plastic_container",
        "플라스틱 용기"
    );

    upsertMapping(
        TFLITE_MODEL_NAME,
        "can",
        "캔"
    );

    upsertMapping(
        TFLITE_MODEL_NAME,
        "glass_bottle",
        "유리병"
    );

    upsertMapping(
        TFLITE_MODEL_NAME,
        "styrofoam",
        "스티로폼"
    );

    log.info(
        "Local TFLite WasteItem mapping seed completed."
    );
  }

  /**
   * 같은 modelName + modelLabel의 매핑이 존재하면
   * WasteItem 연결과 활성 상태를 갱신하고,
   * 존재하지 않으면 새 매핑을 생성합니다.
   */
  private void upsertMapping(
      String modelName,
      String modelLabel,
      String wasteItemName
  ) {
    int updatedCount =
        jdbcTemplate.update(
            """
            UPDATE ai_waste_item_mappings mapping
            JOIN waste_items item
              ON item.name = ?
            SET
              mapping.waste_item_id = item.id,
              mapping.active = TRUE
            WHERE mapping.model_name = ?
              AND mapping.model_label = ?
            """,
            wasteItemName,
            modelName,
            modelLabel
        );

    if (updatedCount > 0) {
      log.info(
          "AI mapping updated. model={}, label={}, wasteItem={}",
          modelName,
          modelLabel,
          wasteItemName
      );

      return;
    }

    jdbcTemplate.update(
        """
        INSERT INTO ai_waste_item_mappings
            (
              active,
              model_name,
              model_label,
              waste_item_id
            )
        SELECT
            TRUE,
            ?,
            ?,
            item.id
        FROM waste_items item
        WHERE item.name = ?
          AND NOT EXISTS (
              SELECT 1
              FROM ai_waste_item_mappings mapping
              WHERE mapping.model_name = ?
                AND mapping.model_label = ?
          )
        """,
        modelName,
        modelLabel,
        wasteItemName,
        modelName,
        modelLabel
    );

    log.info(
        "AI mapping checked. model={}, label={}, wasteItem={}",
        modelName,
        modelLabel,
        wasteItemName
    );
  }
}