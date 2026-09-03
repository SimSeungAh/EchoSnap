package com.echosnap.backend.domain.apartment.entity;

public enum ApartmentStatus {
    /**
     * 사용자가 임시 등록했으며 관리자 검토를 기다리는 상태
     */
    PENDING,

    /**
     * 관리자가 승인하여 일반 사용자가 검색하고 선택할 수 있는 상태
     */
    APPROVED,

    /**
     * 관리자가 등록 요청을 거절한 상태
     */
    REJECTED
}
