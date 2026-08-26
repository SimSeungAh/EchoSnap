package com.smartrecycle.backend.domain.image.dto.response;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

/**
 * 실제 이미지 파일을 Controller에 전달하기 위한
 * 내부 응답 객체입니다.
 *
 * 일반 JSON 응답으로 직렬화하는 DTO가 아니라,
 * Controller가 이미지 바이너리를 반환할 때 사용합니다.
 */
public record ImageFileResponse(

    Resource resource,

    MediaType mediaType

) {
}