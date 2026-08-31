package com.smartrecycle.backend.global.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * local 개발환경에서
 * Python YOLO 모델 label과
 * SmartRecycle WasteItem의 기본 매핑을 생성합니다.
 *
 * YOLO classId를 WasteItem PK로 직접 사용하지 않고
 * modelName + modelLabel 기준으로 명시적으로 연결합니다.
 *
 * ApplicationReadyEvent에서 실행하는 이유:
 *
 * LocalWasteSeedInitializer가 먼저
 * WasteCategory / WasteItem을 생성한 이후에
 * AI 매핑을 등록하기 위해서입니다.
 */
@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class LocalAiMappingSeedInitializer
    implements ApplicationListener<ApplicationReadyEvent> {

  private static final String MODEL_NAME =
      "SMARTRECYCLE_YOLO";

  private final JdbcTemplate jdbcTemplate;

  @Override
  public void onApplicationEvent(
      ApplicationReadyEvent event
  ) {
    seedMappings();
  }

  private void seedMappings() {

    /*
     * 현재 YOLO 학습 클래스 순서와
     * SmartRecycle WasteItem의 연결입니다.
     *
     * cardboard -> 종이박스
     * pet       -> 페트병
     * plastic   -> 플라스틱 용기
     * can       -> 캔
     * glass     -> 유리병
     * styro     -> 스티로폼
     */

    upsertMapping(
        "cardboard",
        "종이박스"
    );

    upsertMapping(
        "pet",
        "페트병"
    );

    upsertMapping(
        "plastic_container",
        "플라스틱 용기"
    );

    upsertMapping(
        "can",
        "캔"
    );

    upsertMapping(
        "glass",
        "유리병"
    );

    upsertMapping(
        "styro",
        "스티로폼"
    );

    log.info(
        "Local YOLO WasteItem mapping seed completed."
    );
  }

  /**
   * 이미 같은 modelName + modelLabel의 매핑이 존재하면
   * WasteItem 연결과 활성 상태를 최신 개발 기준으로 갱신합니다.
   *
   * 존재하지 않으면 새 매핑을 생성합니다.
   */
  private void upsertMapping(
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
            MODEL_NAME,
            modelLabel
        );

    if (updatedCount > 0) {
      log.info(
          "YOLO mapping updated. label={}, wasteItem={}",
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
        MODEL_NAME,
        modelLabel,
        wasteItemName,
        MODEL_NAME,
        modelLabel
    );

    log.info(
        "YOLO mapping checked. label={}, wasteItem={}",
        modelLabel,
        wasteItemName
    );
  }
}