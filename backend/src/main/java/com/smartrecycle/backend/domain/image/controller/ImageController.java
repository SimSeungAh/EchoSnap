package com.smartrecycle.backend.domain.image.controller;

import com.smartrecycle.backend.domain.image.dto.request.CorrectImageResultRequest;
import com.smartrecycle.backend.domain.image.dto.request.RecordMobileAnalysisRequest;
import com.smartrecycle.backend.domain.image.dto.response.ImageCorrectionResponse;
import com.smartrecycle.backend.domain.image.dto.response.ImageFileResponse;
import com.smartrecycle.backend.domain.image.dto.response.ImageUploadResponse;
import com.smartrecycle.backend.domain.image.dto.response.MobileAnalysisResponse;
import com.smartrecycle.backend.domain.image.service.ImageCorrectionService;
import com.smartrecycle.backend.domain.image.service.ImageFileService;
import com.smartrecycle.backend.domain.image.service.ImageUploadService;
import com.smartrecycle.backend.domain.image.service.MobileImageAnalysisService;
import com.smartrecycle.backend.global.response.ApiResponse;
import com.smartrecycle.backend.global.security.service.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

  private final MobileImageAnalysisService
      mobileImageAnalysisService;

  private final ImageCorrectionService
      imageCorrectionService;

  /**
   * AI 분석에 사용할 이미지를 업로드합니다.
   */
  @PostMapping(
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE
  )
  @Operation(
      summary = "AI 분석 이미지 업로드",
      description = """
                    로그인한 사용자가
                    AI 폐기물 분석에 사용할 이미지를 업로드합니다.

                    지원 형식:
                    - JPG
                    - JPEG
                    - PNG

                    최대 파일 크기:
                    - 10MB

                    업로드가 완료되면
                    ImageLog가 UPLOADED 상태로 생성됩니다.
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
   * Flutter TensorFlow Lite의
   * 1차 AI 분석 결과를 기록합니다.
   */
  @PostMapping(
      "/{imageLogId}/mobile-analysis"
  )
  @Operation(
      summary = "모바일 AI 분석 결과 저장",
      description = """
                    Flutter TensorFlow Lite가 수행한
                    1차 폐기물 분류 결과를 저장합니다.

                    신뢰도가 0.70 이상이면
                    MOBILE_ANALYZED 상태가 됩니다.

                    신뢰도가 0.70 미만이면
                    SERVER_REANALYSIS_PENDING 상태가 되어
                    이후 Python YOLO 재분석 대상으로 처리됩니다.
                    """
  )
  public ApiResponse<MobileAnalysisResponse>
  recordMobileAnalysis(

      @AuthenticationPrincipal
      CustomUserDetails userDetails,

      @PathVariable
      Long imageLogId,

      @Valid
      @RequestBody
      RecordMobileAnalysisRequest request
  ) {
    MobileAnalysisResponse response =
        mobileImageAnalysisService
            .recordMobileAnalysis(
                userDetails.getUserId(),
                imageLogId,
                request
            );

    return ApiResponse.success(
        "모바일 AI 분석 결과를 저장했습니다.",
        response
    );
  }

  /**
   * AI 분석 결과가 틀린 경우
   * 사용자가 올바른 품목으로 수정합니다.
   */
  @PutMapping(
      "/{imageLogId}/correction"
  )
  @Operation(
      summary = "AI 분석 결과 사용자 수정",
      description = """
                    로그인한 사용자가
                    자신의 AI 분석 결과가 잘못되었다고 판단한 경우
                    올바른 폐기물 품목을 선택합니다.

                    기존 모바일 TensorFlow Lite 결과와
                    서버 YOLO 결과는 삭제하지 않습니다.

                    사용자 수정 결과는 별도로 저장되며
                    관리자 검수 상태가 PENDING으로 변경됩니다.

                    동일한 ImageLog의 수정 결과를 다시 변경하는 것도
                    같은 API를 통해 처리할 수 있습니다.
                    """
  )
  public ApiResponse<ImageCorrectionResponse>
  correctImageResult(

      @AuthenticationPrincipal
      CustomUserDetails userDetails,

      @PathVariable
      Long imageLogId,

      @Valid
      @RequestBody
      CorrectImageResultRequest request
  ) {
    ImageCorrectionResponse response =
        imageCorrectionService
            .correctResult(
                userDetails.getUserId(),
                imageLogId,
                request
            );

    return ApiResponse.success(
        "AI 분석 결과를 수정했습니다.",
        response
    );
  }

  /**
   * 로그인 사용자가
   * 자신의 업로드 이미지를 조회합니다.
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
        .header(
            HttpHeaders.CACHE_CONTROL,
            "no-store, no-cache, must-revalidate"
        )
        .header(
            "X-Content-Type-Options",
            "nosniff"
        )
        .body(
            response.resource()
        );
  }
}