package com.echosnap.backend.domain.image.entity;

import com.echosnap.backend.domain.waste.entity.WasteItem;
import com.echosnap.backend.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * AI 모델이 반환하는 label과
 * EchoSnap WasteItem을 연결하는 매핑 Entity입니다.
 *
 * 예:
 *
 * YOLO label
 * plastic_bottle
 *
 * ↓
 *
 * WasteItem
 * 투명 페트병
 *
 * AI 모델의 classId와 DB PK는
 * 서로 다른 생명주기를 가지므로 직접 연결하지 않습니다.
 */
@Getter
@Entity
@Table(
    name = "ai_waste_item_mappings",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_ai_waste_item_mapping_model_label",
            columnNames = {
                "model_name",
                "model_label"
            }
        )
    },
    indexes = {
        @Index(
            name = "idx_ai_mapping_model_label",
            columnList = "model_name, model_label"
        ),
        @Index(
            name = "idx_ai_mapping_waste_item",
            columnList = "waste_item_id"
        ),
        @Index(
            name = "idx_ai_mapping_active",
            columnList = "active"
        )
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiWasteItemMapping
    extends BaseEntity {

  @Id
  @GeneratedValue(
      strategy = GenerationType.IDENTITY
  )
  private Long id;

  /**
   * 어떤 AI 모델 체계의 label인지 구분합니다.
   *
   * 예:
   * ECHOSNAP_YOLO
   *
   * 나중에 모바일 TFLite 모델의 label 체계와
   * 서버 YOLO label 체계가 다를 경우에도
   * 충돌하지 않도록 합니다.
   */
  @Column(
      name = "model_name",
      nullable = false,
      length = 100
  )
  private String modelName;

  /**
   * AI 모델이 반환하는 실제 class label
   *
   * 예:
   * plastic_bottle
   * paper_cup
   */
  @Column(
      name = "model_label",
      nullable = false,
      length = 150
  )
  private String modelLabel;

  /**
   * AI label이 실제 서비스에서 의미하는
   * EchoSnap WasteItem입니다.
   */
  @ManyToOne(
      fetch = FetchType.LAZY,
      optional = false
  )
  @JoinColumn(
      name = "waste_item_id",
      nullable = false
  )
  private WasteItem wasteItem;

  /**
   * 현재 AI 분석 결과 매핑에
   * 사용할 수 있는지 여부
   */
  @Column(
      nullable = false
  )
  private boolean active;

  private AiWasteItemMapping(
      String modelName,
      String modelLabel,
      WasteItem wasteItem
  ) {
    this.modelName =
        normalizeRequired(
            modelName
        );

    this.modelLabel =
        normalizeRequired(
            modelLabel
        );

    this.wasteItem =
        wasteItem;

    this.active =
        true;
  }

  /**
   * 새로운 AI label 매핑을 생성합니다.
   */
  public static AiWasteItemMapping create(
      String modelName,
      String modelLabel,
      WasteItem wasteItem
  ) {
    return new AiWasteItemMapping(
        modelName,
        modelLabel,
        wasteItem
    );
  }

  /**
   * 연결되는 WasteItem을 변경합니다.
   *
   * 모델 label 자체는 식별값으로 사용하므로
   * 현재 단계에서는 변경하지 않습니다.
   */
  public void changeWasteItem(
      WasteItem wasteItem
  ) {
    this.wasteItem =
        wasteItem;
  }

  public void activate() {
    this.active =
        true;
  }

  public void deactivate() {
    this.active =
        false;
  }

  private static String normalizeRequired(
      String value
  ) {
    if (
        value == null
            || value.isBlank()
    ) {
      throw new IllegalArgumentException(
          "AI 모델 매핑 값은 필수입니다."
      );
    }

    return value.trim();
  }
}