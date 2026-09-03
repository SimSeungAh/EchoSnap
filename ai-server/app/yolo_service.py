from __future__ import annotations

import os
from io import BytesIO
from pathlib import Path
from typing import Any

from PIL import Image, UnidentifiedImageError
from ultralytics import YOLO


class YoloModelNotReadyError(RuntimeError):
    """
    YOLO 모델 파일이 아직 준비되지 않은 경우 발생합니다.
    """


class InvalidImageError(ValueError):
    """
    정상적인 이미지 파일이 아닌 경우 발생합니다.
    """


class YoloInferenceError(RuntimeError):
    """
    YOLO 추론 과정에서 오류가 발생한 경우입니다.
    """


class YoloPrediction:
    """
    Spring Boot로 전달할 YOLO 분석 결과입니다.
    """

    def __init__(
        self,
        *,
        detected: bool,
        class_id: int | None,
        label: str | None,
        confidence: float | None,
        detection_count: int,
        model_version: str,
    ) -> None:
        self.detected = detected
        self.class_id = class_id
        self.label = label
        self.confidence = confidence
        self.detection_count = detection_count
        self.model_version = model_version


class YoloService:

    DEFAULT_MODEL_PATH = (
        "./models/echosnap-yolo.pt"
    )

    DEFAULT_MODEL_VERSION = (
        "echosnap-yolo-v1"
    )

    DEFAULT_CONFIDENCE_THRESHOLD = 0.25

    MAX_FILE_SIZE = (
        10 * 1024 * 1024
    )

    def __init__(self) -> None:
        configured_model_path = os.getenv(
            "ECHOSNAP_YOLO_MODEL_PATH",
            self.DEFAULT_MODEL_PATH,
        )

        self.model_path = (
            Path(configured_model_path)
            .expanduser()
            .resolve()
        )

        self.model_version = os.getenv(
            "ECHOSNAP_YOLO_MODEL_VERSION",
            self.DEFAULT_MODEL_VERSION,
        ).strip()

        confidence_text = os.getenv(
            "ECHOSNAP_YOLO_CONFIDENCE",
            str(
                self.DEFAULT_CONFIDENCE_THRESHOLD
            ),
        )

        try:
            confidence_threshold = float(
                confidence_text
            )
        except ValueError:
            confidence_threshold = (
                self.DEFAULT_CONFIDENCE_THRESHOLD
            )

        if not 0.0 <= confidence_threshold <= 1.0:
            confidence_threshold = (
                self.DEFAULT_CONFIDENCE_THRESHOLD
            )

        self.confidence_threshold = (
            confidence_threshold
        )

        self._model: YOLO | None = None

    def is_model_file_present(self) -> bool:
        """
        설정된 YOLO 모델 파일이
        실제로 존재하는지 확인합니다.
        """

        return (
            self.model_path.exists()
            and self.model_path.is_file()
        )

    def is_ready(self) -> bool:
        """
        실제 YOLO 모델 분석이 가능한 상태인지 확인합니다.
        """

        return self.is_model_file_present()

    def analyze(
        self,
        image_bytes: bytes,
    ) -> YoloPrediction:
        """
        이미지 byte 데이터를 YOLO로 분석합니다.
        """

        self._validate_file_size(
            image_bytes
        )

        image = self._open_image(
            image_bytes
        )

        model = self._get_model()

        try:
            results = model.predict(
                source=image,
                conf=self.confidence_threshold,
                verbose=False,
            )

        except Exception as exc:
            raise YoloInferenceError(
                "YOLO 이미지 추론에 실패했습니다."
            ) from exc

        if not results:
            return self._empty_prediction()

        result = results[0]

        # EchoSnap의 최종 학습 모델이
        # Classification 또는 Detection 중
        # 어떤 방식으로 확정될지 아직 정해지지 않았으므로
        # 두 결과 형식을 모두 처리합니다.

        classification_prediction = (
            self._extract_classification_prediction(
                result
            )
        )

        if classification_prediction is not None:
            return classification_prediction

        detection_prediction = (
            self._extract_detection_prediction(
                result
            )
        )

        if detection_prediction is not None:
            return detection_prediction

        return self._empty_prediction()

    def _get_model(self) -> YOLO:
        """
        모델은 최초 분석 요청 시 한 번 로드하고
        이후 같은 인스턴스를 재사용합니다.
        """

        if self._model is not None:
            return self._model

        if not self.is_model_file_present():
            raise YoloModelNotReadyError(
                "EchoSnap YOLO 모델 파일을 "
                f"찾을 수 없습니다: {self.model_path}"
            )

        try:
            self._model = YOLO(
                str(self.model_path)
            )

        except Exception as exc:
            raise YoloModelNotReadyError(
                "EchoSnap YOLO 모델을 "
                "로드할 수 없습니다."
            ) from exc

        return self._model

    def _open_image(
        self,
        image_bytes: bytes,
    ) -> Image.Image:
        """
        실제 이미지 데이터를 Pillow로 검증합니다.
        """

        if not image_bytes:
            raise InvalidImageError(
                "이미지 파일이 비어 있습니다."
            )

        try:
            image = Image.open(
                BytesIO(image_bytes)
            )

            image.verify()

            image = Image.open(
                BytesIO(image_bytes)
            )

            return image.convert("RGB")

        except (
            UnidentifiedImageError,
            OSError,
        ) as exc:
            raise InvalidImageError(
                "정상적인 이미지 파일이 아닙니다."
            ) from exc

    def _validate_file_size(
        self,
        image_bytes: bytes,
    ) -> None:

        if not image_bytes:
            raise InvalidImageError(
                "이미지 파일이 비어 있습니다."
            )

        if (
            len(image_bytes)
            > self.MAX_FILE_SIZE
        ):
            raise InvalidImageError(
                "이미지 파일의 최대 크기는 "
                "10MB입니다."
            )

    def _extract_classification_prediction(
        self,
        result: Any,
    ) -> YoloPrediction | None:
        """
        YOLO Classification 결과를 처리합니다.
        """

        probs = getattr(
            result,
            "probs",
            None,
        )

        if probs is None:
            return None

        top1 = getattr(
            probs,
            "top1",
            None,
        )

        top1_conf = getattr(
            probs,
            "top1conf",
            None,
        )

        if (
            top1 is None
            or top1_conf is None
        ):
            return None

        class_id = int(top1)

        confidence = float(
            top1_conf.item()
            if hasattr(
                top1_conf,
                "item",
            )
            else top1_conf
        )

        label = self._resolve_label(
            result,
            class_id,
        )

        return YoloPrediction(
            detected=True,
            class_id=class_id,
            label=label,
            confidence=confidence,
            detection_count=1,
            model_version=self.model_version,
        )

    def _extract_detection_prediction(
        self,
        result: Any,
    ) -> YoloPrediction | None:
        """
        Object Detection 결과에서
        confidence가 가장 높은 객체를
        대표 결과로 선택합니다.
        """

        boxes = getattr(
            result,
            "boxes",
            None,
        )

        if boxes is None:
            return None

        confidence_values = getattr(
            boxes,
            "conf",
            None,
        )

        class_values = getattr(
            boxes,
            "cls",
            None,
        )

        if (
            confidence_values is None
            or class_values is None
            or len(confidence_values) == 0
        ):
            return None

        best_index = int(
            confidence_values.argmax().item()
        )

        best_confidence = float(
            confidence_values[
                best_index
            ].item()
        )

        class_id = int(
            class_values[
                best_index
            ].item()
        )

        label = self._resolve_label(
            result,
            class_id,
        )

        return YoloPrediction(
            detected=True,
            class_id=class_id,
            label=label,
            confidence=best_confidence,
            detection_count=len(
                confidence_values
            ),
            model_version=self.model_version,
        )

    def _resolve_label(
        self,
        result: Any,
        class_id: int,
    ) -> str:
        """
        YOLO class ID를 label 문자열로 변환합니다.
        """

        names = getattr(
            result,
            "names",
            None,
        )

        if names is None:
            return str(class_id)

        if isinstance(
            names,
            dict,
        ):
            return str(
                names.get(
                    class_id,
                    class_id,
                )
            )

        if isinstance(
            names,
            (
                list,
                tuple,
            ),
        ):
            if (
                0
                <= class_id
                < len(names)
            ):
                return str(
                    names[class_id]
                )

        return str(class_id)

    def _empty_prediction(
        self,
    ) -> YoloPrediction:

        return YoloPrediction(
            detected=False,
            class_id=None,
            label=None,
            confidence=None,
            detection_count=0,
            model_version=self.model_version,
        )


yolo_service = YoloService()