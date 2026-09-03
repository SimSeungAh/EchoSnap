package com.echosnap.backend.domain.collectionarea.dto.response;

/**
 * 행정안전부 생활쓰레기배출정보를
 * CollectionArea에 동기화한 결과입니다.
 */
public record CollectionAreaSyncResultResponse(

    int sourceTotalCount,

    int fetchedCount,

    int createdCount,

    int updatedCount,

    int skippedCount,

    int pageCount

) {
}