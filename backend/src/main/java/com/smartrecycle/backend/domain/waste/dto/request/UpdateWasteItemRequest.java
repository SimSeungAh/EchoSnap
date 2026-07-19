package com.smartrecycle.backend.domain.waste.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdateWasteItemRequest(

        /**
         * 품목이 속할 폐기물 카테고리 ID
         * 품목 수정 시 다른 카테고리로 이동할 수도 있음
         */
        @NotNull(message = "카테고리 ID는 필수입니다.")
        @Positive(message = "카테고리 ID는 1 이상이어야 합니다.")
        Long categoryId,

        /**
         * 사용자 화면과 검색 결과에 표시할 품목명
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
         */
        @Size(
                max = 500,
                message = "검색 키워드는 500자 이하여야 합니다."
        )
        String searchKeywords,

        /**
         * 품목 대표 이미지 주소
         * 절대 URL과 상대 경로를 모두 허용
         */
        @Size(
                max = 500,
                message = "대표 이미지 주소는 500자 이하여야 합니다."
        )
        String imageUrl
) {
}