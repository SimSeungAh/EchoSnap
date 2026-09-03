package com.echosnap.backend.domain.image.controller;

import com.echosnap.backend.domain.image.dto.request.CorrectImageResultRequest;
import com.echosnap.backend.domain.image.dto.request.RecordMobileAnalysisRequest;
import com.echosnap.backend.domain.image.dto.response.ImageCorrectionResponse;
import com.echosnap.backend.domain.image.dto.response.ImageFileResponse;
import com.echosnap.backend.domain.image.dto.response.ImageUploadResponse;
import com.echosnap.backend.domain.image.dto.response.MobileAnalysisResponse;
import com.echosnap.backend.domain.image.dto.response.ServerReanalysisResponse;
import com.echosnap.backend.domain.image.service.ImageCorrectionService;
import com.echosnap.backend.domain.image.service.ImageFileService;
import com.echosnap.backend.domain.image.service.ImageUploadService;
import com.echosnap.backend.domain.image.service.MobileImageAnalysisService;
import com.echosnap.backend.domain.image.service.ServerImageAnalysisService;
import com.echosnap.backend.global.response.ApiResponse;
import com.echosnap.backend.global.security.service.CustomUserDetails;
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

  private final ServerImageAnalysisService
      serverImageAnalysisService;

  /**
   * AI 분석에 사용할 이미지 업로드
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

                    업로드 완료 시
                    ImageLog는 UPLOADED 상태가 됩니다.
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
   * Flutter TensorFlow Lite 분석 결과 저장
   */
  @PostMapping(
      "/{imageLogId}/mobile-analysis"
  )
  @Operation(
      summary = "모바일 AI 분석 결과 저장",
      description = """
                    Flutter TensorFlow Lite의
                    1차 분석 결과를 저장합니다.

                    신뢰도가 기준 이상이면
                    MOBILE_ANALYZED 상태가 됩니다.

                    신뢰도가 기준보다 낮으면
                    SERVER_REANALYSIS_PENDING으로 변경됩니다.
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
   * 낮은 신뢰도의 이미지를
   * Python YOLO 서버로 재분석합니다.
   */
  @PostMapping(
      "/{imageLogId}/server-reanalysis"
  )
  @Operation(
      summary = "Python YOLO 서버 재분석",
      description = """
                    모바일 TensorFlow Lite 분석 신뢰도가 낮아
                    SERVER_REANALYSIS_PENDING 상태가 된 이미지를
                    Python FastAPI YOLO 서버로 전송합니다.

                    흐름:

                    1. 로그인 사용자와 ImageLog 소유권 확인
                    2. SERVER_REANALYSIS_PENDING 상태 확인
                    3. 저장된 실제 이미지 로드
                    4. FastAPI /analyze multipart 호출
                    5. YOLO label을 WasteItem으로 매핑
                    6. 서버 AI 결과와 신뢰도, 모델 버전 저장

                    YOLO의 classId는
                    WasteItem DB ID로 직접 사용하지 않습니다.
                    """
  )
  public ApiResponse<ServerReanalysisResponse>
  reanalyzeWithServerAi(

      @AuthenticationPrincipal
      CustomUserDetails userDetails,

      @PathVariable
      Long imageLogId
  ) {
    ServerReanalysisResponse response =
        serverImageAnalysisService
            .reanalyze(
                userDetails.getUserId(),
                imageLogId
            );

    return ApiResponse.success(
        "서버 AI 재분석을 완료했습니다.",
        response
    );
  }

  /**
   * AI 분석 결과 사용자 수정
   */
  @PutMapping(
      "/{imageLogId}/correction"
  )
  @Operation(
      summary = "AI 분석 결과 사용자 수정",
      description = """
                    사용자가 AI 분석 결과가 잘못되었다고 판단한 경우
                    올바른 폐기물 품목으로 수정합니다.

                    기존 AI 분석 결과는 삭제하지 않고
                    사용자 수정값을 별도로 저장합니다.

                    수정된 데이터는
                    관리자 검수 PENDING 상태가 됩니다.
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
   * 로그인 사용자의 이미지 파일 조회
   */
  @GetMapping(
      "/files/{storedFileName}"
  )
  @Operation(
      summary = "내 이미지 파일 조회",
      description = """
                    로그인한 사용자가
                    자신이 업로드한 이미지 파일을 조회합니다.

                    다른 사용자의 이미지에는 접근할 수 없습니다.
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