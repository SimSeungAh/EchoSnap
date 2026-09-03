package com.echosnap.backend.domain.waste.entity;

import com.echosnap.backend.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "recycle_guides",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_recycle_guides_waste_item",
                        columnNames = "waste_item_id"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecycleGuide extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 가이드가 연결된 폐기물 품목
     * 하나의 폐기물 품목에는 하나의 대표 가이드만 등록할 수 있음
     */
    @OneToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "waste_item_id",
            nullable = false,
            unique = true
    )
    private WasteItem wasteItem;

    /**
     * 목록이나 상세 화면 상단에 표시할 짧은 설명
     * 예:
     * 내용물을 비우고 라벨을 제거해 배출합니다.
     */
    @Column(
            nullable = false,
            length = 500
    )
    private String summary;

    /**
     * 상세한 분리배출 방법
     * 예:
     * 내용물을 비운 후 라벨을 제거하고
     * 찌그러뜨려 전용 수거함에 배출합니다.
     */
    @Column(
            name = "disposal_method",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String disposalMethod;

    /**
     * 주의 사항
     * 예:
     * 이물질이 심하게 묻은 경우
     * 일반 종량제 봉투로 배출합니다.
     */
    @Column(
            columnDefinition = "TEXT"
    )
    private String caution;

    private RecycleGuide(
            WasteItem wasteItem,
            String summary,
            String disposalMethod,
            String caution
    ) {
        this.wasteItem = wasteItem;
        this.summary = summary;
        this.disposalMethod = disposalMethod;
        this.caution = caution;
    }

    /**
     * 새로운 분리배출 가이드를 생성
     */
    public static RecycleGuide create(
            WasteItem wasteItem,
            String summary,
            String disposalMethod,
            String caution
    ) {
        return new RecycleGuide(
                wasteItem,
                summary,
                disposalMethod,
                caution
        );
    }

    /**
     * 기존 분리배출 가이드 내용을 수정
     * 관리자 가이드 등록 또는 수정 API에서 사용
     */
    public void update(
            String summary,
            String disposalMethod,
            String caution
    ) {
        this.summary = summary;
        this.disposalMethod = disposalMethod;
        this.caution = caution;
    }
}