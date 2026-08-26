package com.smartrecycle.backend.domain.image.storage;

/**
 * 로컬 이미지 저장 결과입니다.
 *
 * 서버의 실제 절대 경로는
 * 사용자에게 노출하지 않습니다.
 */
public record StoredImageFile(

    /**
     * 서버가 UUID로 생성한
     * 실제 저장 파일명
     */
    String storedFileName,

    /**
     * 저장된 파일의 크기(byte)
     */
    long fileSize

) {
}