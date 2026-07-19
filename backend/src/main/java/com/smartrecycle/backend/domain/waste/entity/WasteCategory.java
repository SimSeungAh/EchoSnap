package com.smartrecycle.backend.domain.waste.entity;

import com.smartrecycle.backend.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "waste_categories",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_waste_categories_code",
                        columnNames = "code"
                )
        },
        indexes = {
                @Index(
                        name = "idx_waste_categories_name",
                        columnList = "name"
                ),
                @Index(
                        name = "idx_waste_categories_active",
                        columnList = "active"
                ),
                @Index(
                        name = "idx_waste_categories_sort_order",
                        columnList = "sort_order"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WasteCategory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 폐기물 카테고리 식별 코드
     * 예:
     * PLASTIC
     * PAPER
     * GLASS
     */
    @Column(
            nullable = false,
            length = 30
    )
    private String code;

    /**
     * 사용자에게 표시할 카테고리 이름
     * 예:
     * 플라스틱
     * 종이류
     * 유리병류
     */
    @Column(
            nullable = false,
            length = 50
    )
    private String name;

    /**
     * 카테고리에 대한 설명
     */
    @Column(length = 500)
    private String description;

    /**
     * 카테고리 표시 순서
     * 숫자가 작을수록 먼저 표시
     */
    @Column(
            name = "sort_order",
            nullable = false
    )
    private int sortOrder;

    /**
     * 사용자 화면 노출 여부
     */
    @Column(nullable = false)
    private boolean active;

    private WasteCategory(
            String code,
            String name,
            String description,
            int sortOrder
    ) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.sortOrder = sortOrder;
        this.active = true;
    }

    /**
     * 폐기물 카테고리를 생성
     */
    public static WasteCategory create(
            String code,
            String name,
            String description,
            int sortOrder
    ) {
        return new WasteCategory(
                code,
                name,
                description,
                sortOrder
        );
    }

    /**
     * 폐기물 카테고리 정보를 수정
     */
    public void update(
            String code,
            String name,
            String description,
            int sortOrder,
            boolean active
    ) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.sortOrder = sortOrder;
        this.active = active;
    }

    /**
     * 사용자 화면에서 카테고리를 숨김
     */
    public void deactivate() {
        this.active = false;
    }

    /**
     * 숨긴 카테고리를 다시 노출
     */
    public void activate() {
        this.active = true;
    }
}