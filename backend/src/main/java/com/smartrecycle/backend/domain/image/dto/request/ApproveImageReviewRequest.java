package com.smartrecycle.backend.domain.image.dto.request;

import jakarta.validation.constraints.Size;

/**
 * 관리자가 사용자 수정 AI 데이터를
 * 승인할 때 사용하는 요청입니다.
 */
public record ApproveImageReviewRequest(

    /**
     * 승인 사유나 간단한 검수 메모입니다.
     *
     * 승인 자체에는 필수가 아니므로
     * 선택값으로 둡니다.
     */
    @Size(
        max = 1000,
        message = "검수 메모는 1000자 이하여야 합니다."
    )
    String reviewNote

) {
}