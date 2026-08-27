from fastapi import (
    FastAPI,
    File,
    HTTPException,
    UploadFile,
    status,
)
from pydantic import BaseModel

from app.yolo_service import (
    InvalidImageError,
    YoloInferenceError,
    YoloModelNotReadyError,
    yolo_service,
)


app = FastAPI(
    title="SmartRecycle AI Server",
    description="SmartRecycle 이미지 YOLO 분석 서버",
    version="0.2.0",
)


class HealthResponse(BaseModel):
    status: str
    service: str
    modelReady: bool
    modelVersion: str


class AnalyzeResponse(BaseModel):
    detected: bool
    classId: int | None
    label: str | None
    confidence: float | None
    detectionCount: int
    modelVersion: str


@app.get(
    "/health",
    response_model=HealthResponse,
)
def health_check() -> HealthResponse:
    """
    FastAPI 자체 상태와
    YOLO 모델 준비 여부를 확인합니다.
    """

    model_ready = yolo_service.is_ready()

    return HealthResponse(
        status=(
            "ok"
            if model_ready
            else "degraded"
        ),
        service="smart-recycle-ai-server",
        modelReady=model_ready,
        modelVersion=yolo_service.model_version,
    )


@app.post(
    "/analyze",
    response_model=AnalyzeResponse,
)
async def analyze_image(
    file: UploadFile = File(...),
) -> AnalyzeResponse:
    """
    Spring Boot가 전달한 이미지를
    실제 YOLO 모델로 분석합니다.

    지원 형식:
    - JPEG
    - PNG

    최대 파일 크기:
    - 10MB
    """

    allowed_content_types = {
        "image/jpeg",
        "image/png",
    }

    if file.content_type not in allowed_content_types:
        raise HTTPException(
            status_code=(
                status.HTTP_415_UNSUPPORTED_MEDIA_TYPE
            ),
            detail=(
                "JPG, JPEG, PNG 이미지만 "
                "분석할 수 있습니다."
            ),
        )

    image_bytes = await file.read()

    try:
        prediction = yolo_service.analyze(
            image_bytes
        )

    except InvalidImageError as exc:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=str(exc),
        ) from exc

    except YoloModelNotReadyError as exc:
        raise HTTPException(
            status_code=(
                status.HTTP_503_SERVICE_UNAVAILABLE
            ),
            detail=str(exc),
        ) from exc

    except YoloInferenceError as exc:
        raise HTTPException(
            status_code=(
                status.HTTP_500_INTERNAL_SERVER_ERROR
            ),
            detail=str(exc),
        ) from exc

    return AnalyzeResponse(
        detected=prediction.detected,
        classId=prediction.class_id,
        label=prediction.label,
        confidence=prediction.confidence,
        detectionCount=prediction.detection_count,
        modelVersion=prediction.model_version,
    )