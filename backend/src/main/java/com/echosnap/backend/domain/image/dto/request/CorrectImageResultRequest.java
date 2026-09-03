package com.echosnap.backend.domain.image.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * 사용자가 AI 분석 결과가 잘못되었다고 판단했을 때
 * 올바른 폐기물 품목을 선택하는 요청입니다.
 */
public record CorrectImageResultRequest(

    /**
     * 사용자가 직접 선택한
     * 올바른 WasteItem ID
     */
    @NotNull(
        message = "수정할 폐기물 품목은 필수입니다."
    )
    Long wasteItemId

) {
}