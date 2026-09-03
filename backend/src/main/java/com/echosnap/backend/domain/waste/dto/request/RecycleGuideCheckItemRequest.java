package com.echosnap.backend.domain.waste.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record RecycleGuideCheckItemRequest(

        /**
         * 사용자에게 표시할 체크리스트 문구
         * 예:
         * 내용물을 비웠나요?
         * 라벨을 제거했나요?
         */
        @NotBlank(message = "체크리스트 내용은 필수입니다.")
        @Size(
                max = 255,
                message = "체크리스트 내용은 255자 이하여야 합니다."
        )
        String content,

        /**
         * 체크리스트 표시 순서
         * 숫자가 작을수록 먼저 표시
         */
        @NotNull(message = "체크리스트 표시 순서는 필수입니다.")
        @PositiveOrZero(
                message = "체크리스트 표시 순서는 0 이상이어야 합니다."
        )
        Integer sortOrder,

        /**
         * 필수 확인 항목 여부
         * true  : 필수 항목
         * false : 권장 항목
         */
        @NotNull(message = "체크리스트 필수 여부는 필수입니다.")
        Boolean required

) {
}