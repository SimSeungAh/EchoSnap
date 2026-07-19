package com.smartrecycle.backend.domain.waste.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CreateWasteCategoryRequest(

        /**
         * 카테고리 식별 코드
         * 영문 대문자로 시작하고 영문 대문자, 숫자, 밑줄만 사용할 수 있음
         * 예:
         * PLASTIC
         * PAPER
         * FOOD_WASTE
         */
        @NotBlank(message = "카테고리 코드는 필수입니다.")
        @Size(
                max = 30,
                message = "카테고리 코드는 30자 이하여야 합니다."
        )
        @Pattern(
                regexp = "^[A-Z][A-Z0-9_]*$",
                message = "카테고리 코드는 영문 대문자로 시작하고 영문 대문자, 숫자, 밑줄만 사용할 수 있습니다."
        )
        String code,

        /**
         * 사용자 화면에 표시할 카테고리 이름
         * 예:
         * 플라스틱
         * 종이류
         * 음식물 쓰레기
         */
        @NotBlank(message = "카테고리 이름은 필수입니다.")
        @Size(
                max = 50,
                message = "카테고리 이름은 50자 이하여야 합니다."
        )
        String name,

        /**
         * 카테고리에 대한 간단한 설명
         */
        @Size(
                max = 500,
                message = "카테고리 설명은 500자 이하여야 합니다."
        )
        String description,

        /**
         * 카테고리 표시 순서
         * 0 이상의 숫자만 허용
         */
        @NotNull(message = "표시 순서는 필수입니다.")
        @PositiveOrZero(message = "표시 순서는 0 이상이어야 합니다.")
        Integer sortOrder

) {
}