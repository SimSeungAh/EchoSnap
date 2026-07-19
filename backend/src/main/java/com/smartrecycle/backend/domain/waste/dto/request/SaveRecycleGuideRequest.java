package com.smartrecycle.backend.domain.waste.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SaveRecycleGuideRequest(

        /**
         * 품목 상세 화면 상단에 표시할 짧은 안내 문구
         * 예:
         * 내용물을 비우고 라벨을 제거해 배출합니다.
         */
        @NotBlank(message = "가이드 요약은 필수입니다.")
        @Size(
                max = 500,
                message = "가이드 요약은 500자 이하여야 합니다."
        )
        String summary,

        /**
         * 품목의 상세한 분리배출 방법
         * 예:
         * 내용물을 비운 후 라벨을 제거하고
         * 찌그러뜨려 전용 수거함에 배출합니다.
         */
        @NotBlank(message = "분리배출 방법은 필수입니다.")
        @Size(
                max = 5000,
                message = "분리배출 방법은 5000자 이하여야 합니다."
        )
        String disposalMethod,

        /**
         * 잘못 배출하기 쉬운 상황이나 예외 사항
         * 필수값은 아니므로 null 또는 빈 문자열을 허용
         */
        @Size(
                max = 5000,
                message = "주의 사항은 5000자 이하여야 합니다."
        )
        String caution,

        /**
         * 분리배출 전에 확인할 체크리스트
         * 최소 1개에서 최대 20개까지 등록할 수 있음
         * 각 항목 내부의 유효성 검사는 @Valid로 실행
         */
        @NotNull(message = "체크리스트는 필수입니다.")
        @Size(
                min = 1,
                max = 20,
                message = "체크리스트는 1개 이상 20개 이하로 등록해야 합니다."
        )
        List<@Valid RecycleGuideCheckItemRequest> checkItems

) {
}