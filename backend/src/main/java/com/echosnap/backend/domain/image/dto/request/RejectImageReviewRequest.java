package com.echosnap.backend.domain.image.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 관리자가 사용자 수정 AI 데이터를
 * 거절할 때 사용하는 요청입니다.
 */
public record RejectImageReviewRequest(

    /**
     * 거절은 이유를 추적할 필요가 있으므로
     * 검수 메모를 필수로 받습니다.
     */
    @NotBlank(
        message = "거절 사유는 필수입니다."
    )
    @Size(
        max = 1000,
        message = "거절 사유는 1000자 이하여야 합니다."
    )
    String reviewNote

) {
}