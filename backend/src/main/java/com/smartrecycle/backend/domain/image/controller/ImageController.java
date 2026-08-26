package com.smartrecycle.backend.domain.image.controller;

import com.smartrecycle.backend.domain.image.dto.response.ImageFileResponse;
import com.smartrecycle.backend.domain.image.dto.response.ImageUploadResponse;
import com.smartrecycle.backend.domain.image.service.ImageFileService;
import com.smartrecycle.backend.domain.image.service.ImageUploadService;
import com.smartrecycle.backend.global.response.ApiResponse;
import com.smartrecycle.backend.global.security.service.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
@Tag(
    name = "Image",
    description = "사용자 이미지 업로드 및 AI 분석 이력 API"
)
public class ImageController {

  private final ImageUploadService
      imageUploadService;

  private final ImageFileService
      imageFileService;

  /**
   * AI 분석에 사용할 이미지를 업로드합니다.
   *
   * 이 단계에서는 이미지 파일과 ImageLog만 생성하며,
   * 실제 AI 분석은 아직 실행하지 않습니다.
   */
  @PostMapping(
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE
  )
  @Operation(
      summary = "AI 분석 이미지 업로드",
      description = """
                    로그인한 사용자가
                    AI 폐기물 분석에 사용할 이미지를 업로드합니다.

                    현재 지원 형식:
                    - JPG
                    - JPEG
                    - PNG

                    최대 파일 크기:
                    - 10MB

                    서버는 클라이언트가 전달한 파일명을
                    실제 저장 파일명으로 사용하지 않고
                    UUID 기반 파일명으로 저장합니다.

                    Content-Type뿐 아니라
                    파일 시작 바이트도 함께 검사하여
                    실제 이미지 형식과 일치하는지 확인합니다.

                    업로드가 완료되면
                    ImageLog가 UPLOADED 상태로 생성됩니다.

                    실제 TensorFlow Lite 분석 결과 저장은
                    이후 API에서 처리합니다.
                    """
  )
  public ApiResponse<ImageUploadResponse>
  uploadImage(

      @AuthenticationPrincipal
      CustomUserDetails userDetails,

      @RequestPart("file")
      MultipartFile file
  ) {
    ImageUploadResponse response =
        imageUploadService
            .uploadImage(
                userDetails.getUserId(),
                file
            );

    return ApiResponse.success(
        "이미지를 업로드했습니다.",
        response
    );
  }

  /**
   * 로그인 사용자가
   * 자신의 업로드 이미지를 조회합니다.
   *
   * imageUrl에 저장되는 주소와 동일한 형식입니다.
   *
   * 예:
   *
   * /api/images/files/
   * 550e8400-e29b-41d4-a716-446655440000.jpg
   */
  @GetMapping(
      "/files/{storedFileName}"
  )
  @Operation(
      summary = "내 이미지 파일 조회",
      description = """
                    로그인한 사용자가
                    자신이 업로드한 이미지 파일을 조회합니다.

                    UUID 파일명을 알고 있더라도
                    해당 ImageLog의 소유자가 아니면
                    파일을 반환하지 않습니다.

                    JPEG 또는 PNG 바이너리를 직접 반환합니다.
                    """
  )
  public ResponseEntity<Resource>
  getMyImageFile(

      @AuthenticationPrincipal
      CustomUserDetails userDetails,

      @PathVariable
      String storedFileName
  ) {
    ImageFileResponse response =
        imageFileService
            .getMyImageFile(
                userDetails.getUserId(),
                storedFileName
            );

    return ResponseEntity
        .ok()
        .contentType(
            response.mediaType()
        )
        /*
         * 사용자의 업로드 이미지는
         * 개인 데이터이므로 브라우저나
         * 중간 캐시에 장기간 남지 않도록 합니다.
         */
        .header(
            HttpHeaders.CACHE_CONTROL,
            "no-store, no-cache, must-revalidate"
        )
        /*
         * 브라우저가 Content-Type을 임의로 추측하지 않고
         * 서버가 지정한 이미지 형식을 따르도록 합니다.
         */
        .header(
            "X-Content-Type-Options",
            "nosniff"
        )
        .body(
            response.resource()
        );
  }
}