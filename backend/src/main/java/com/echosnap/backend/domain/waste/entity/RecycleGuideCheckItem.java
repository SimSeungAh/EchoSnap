package com.echosnap.backend.domain.waste.entity;

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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "recycle_guide_check_items",
        indexes = {
                @Index(
                        name = "idx_recycle_guide_check_items_guide",
                        columnList = "recycle_guide_id"
                ),
                @Index(
                        name = "idx_recycle_guide_check_items_sort_order",
                        columnList = "recycle_guide_id, sort_order"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecycleGuideCheckItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 체크리스트가 속한 분리배출 가이드
     * 하나의 가이드에는 여러 개의 체크 항목이 등록될 수 있음
     */
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "recycle_guide_id",
            nullable = false
    )
    private RecycleGuide recycleGuide;

    /**
     * 사용자에게 표시할 체크리스트 문구
     * 예:
     * 내용물을 비웠나요?
     * 라벨을 제거했나요?
     */
    @Column(
            nullable = false,
            length = 255
    )
    private String content;

    /**
     * 체크리스트 표시 순서
     * 숫자가 작을수록 먼저 표시
     */
    @Column(
            name = "sort_order",
            nullable = false
    )
    private int sortOrder;

    /**
     * 반드시 확인해야 하는 필수 항목인지 여부
     * true  : 필수 체크 항목
     * false : 권장 체크 항목
     */
    @Column(nullable = false)
    private boolean required;

    private RecycleGuideCheckItem(
            RecycleGuide recycleGuide,
            String content,
            int sortOrder,
            boolean required
    ) {
        this.recycleGuide = recycleGuide;
        this.content = content;
        this.sortOrder = sortOrder;
        this.required = required;
    }

    /**
     * 새로운 가이드 체크리스트 항목을 생성
     */
    public static RecycleGuideCheckItem create(
            RecycleGuide recycleGuide,
            String content,
            int sortOrder,
            boolean required
    ) {
        return new RecycleGuideCheckItem(
                recycleGuide,
                content,
                sortOrder,
                required
        );
    }

    /**
     * 기존 체크리스트 내용을 수정
     */
    public void update(
            String content,
            int sortOrder,
            boolean required
    ) {
        this.content = content;
        this.sortOrder = sortOrder;
        this.required = required;
    }
}