package com.smartrecycle.backend.domain.waste.entity;

import com.smartrecycle.backend.global.entity.BaseEntity;
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

@Getter
@Entity
@Table(
        name = "waste_items",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_waste_items_category_name",
                        columnNames = {
                                "category_id",
                                "name"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_waste_items_name",
                        columnList = "name"
                ),
                @Index(
                        name = "idx_waste_items_category",
                        columnList = "category_id"
                ),
                @Index(
                        name = "idx_waste_items_active",
                        columnList = "active"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WasteItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 품목이 속한 폐기물 카테고리
     * 예:
     * 투명 페트병 -> 플라스틱
     * 종이컵 -> 종이류
     */
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "category_id",
            nullable = false
    )
    private WasteCategory category;

    /**
     * 사용자 화면과 검색 결과에 표시할 품목명
     * 같은 카테고리 안에서는 동일한 이름을 중복 등록할 수 없음
     */
    @Column(
            nullable = false,
            length = 100
    )
    private String name;

    /**
     * 품목 검색에 사용할 추가 키워드
     * 여러 검색어를 쉼표로 구분하여 저장
     * 예:
     * 생수병,음료수병,페트병
     */
    @Column(
            name = "search_keywords",
            length = 500
    )
    private String searchKeywords;

    /**
     * 품목의 대표 이미지 주소
     * 초기에는 로컬 이미지 경로나 URL을 사용하고, 추후 이미지 저장 기능에서 S3 주소로 교체할 수 있음
     */
    @Column(
            name = "image_url",
            length = 500
    )
    private String imageUrl;

    /**
     * 일반 사용자 화면 노출 여부
     * false인 품목은 사용자 검색 결과에서 제외하고 관리자 기능에서는 계속 관리
     */
    @Column(nullable = false)
    private boolean active;

    private WasteItem(
            WasteCategory category,
            String name,
            String searchKeywords,
            String imageUrl
    ) {
        this.category = category;
        this.name = name;
        this.searchKeywords = searchKeywords;
        this.imageUrl = imageUrl;
        this.active = true;
    }

    /**
     * 새로운 폐기물 품목을 생성
     */
    public static WasteItem create(
            WasteCategory category,
            String name,
            String searchKeywords,
            String imageUrl
    ) {
        return new WasteItem(
                category,
                name,
                searchKeywords,
                imageUrl
        );
    }

    /**
     * 관리자가 폐기물 품목 정보를 수정
     */
    public void update(
            WasteCategory category,
            String name,
            String searchKeywords,
            String imageUrl
    ) {
        this.category = category;
        this.name = name;
        this.searchKeywords = searchKeywords;
        this.imageUrl = imageUrl;
    }

    /**
     * 품목을 일반 사용자 검색 결과에서 숨김
     */
    public void deactivate() {
        this.active = false;
    }

    /**
     * 비활성화된 품목을 다시 노출할 때 사용
     */
    public void activate() {
        this.active = true;
    }
}