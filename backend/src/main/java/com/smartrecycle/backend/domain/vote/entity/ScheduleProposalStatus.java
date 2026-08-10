package com.smartrecycle.backend.domain.vote.entity;

/**
 * 주민 배출 일정 제안의 처리 상태입니다.
 */
public enum ScheduleProposalStatus {

    /**
     * 주민이 제안을 등록했으며
     * 다른 주민의 투표와 관리자 검토를 기다리는 상태
     */
    PENDING,

    /**
     * 관리자가 제안을 승인한 상태
     *
     * 관리자 선택에 따라 승인된 제안 내용이
     * 공식 배출 일정에 반영될 수 있습니다.
     */
    APPROVED,

    /**
     * 관리자가 제안을 거절한 상태
     */
    REJECTED,

    /**
     * 제안자가 관리자 검토 전에
     * 자신의 제안을 취소한 상태
     */
    CANCELLED
}