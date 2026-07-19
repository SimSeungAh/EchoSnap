package com.smartrecycle.backend.domain.waste.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateWasteItemRequest(

        /**
         * 품목이 속할 폐기물 카테고리 ID
         * 예:
         * 플라스틱 카테고리 ID
         * 종이류 카테고리 ID
         */
        @NotNull(message = "카테고리 ID는 필수입니다.")
        @Positive(message = "카테고리 ID는 1 이상이어야 합니다.")
        Long categoryId,

        /**
         * 사용자 화면과 검색 결과에 표시할 품목명
         * 예:
         * 투명 페트병
         * 종이컵
         * 유리병
         */
        @NotBlank(message = "폐기물 품목명은 필수입니다.")
        @Size(
                max = 100,
                message = "폐기물 품목명은 100자 이하여야 합니다."
        )
        String name,

        /**
         * 품목 검색에 사용할 추가 검색어
         * 여러 검색어는 쉼표로 구분
         * 예:
         * 생수병,음료수병,페트병
         */
        @Size(
                max = 500,
                message = "검색 키워드는 500자 이하여야 합니다."
        )
        String searchKeywords,

        /**
         * 품목 대표 이미지 주소
         * 절대 URL뿐 아니라 다음과 같은 상대 경로도 사용할 수 있으므로 URL 형식 검증은 적용하지 않음
         * 예:
         * /images/waste/pet-bottle.png
         */
        @Size(
                max = 500,
                message = "대표 이미지 주소는 500자 이하여야 합니다."
        )
        String imageUrl
) {
}