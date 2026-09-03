from __future__ import annotations

import argparse
import shutil
from pathlib import Path

import torch
import yaml
from ultralytics import YOLO


AI_SERVER_DIR = Path(__file__).resolve().parent.parent

DATASET_DIR = AI_SERVER_DIR / "dataset"
DATA_CONFIG_PATH = DATASET_DIR / "data.yaml"

MODELS_DIR = AI_SERVER_DIR / "models"
RUNS_DIR = AI_SERVER_DIR / "runs"


EXPECTED_CLASSES = [
    "cardboard_box",
    "pet_bottle",
    "plastic_container",
    "can",
    "glass_bottle",
    "styrofoam",
]


IMAGE_EXTENSIONS = {
    ".jpg",
    ".jpeg",
    ".png",
    ".bmp",
    ".webp",
}


def parse_args():
    parser = argparse.ArgumentParser(
        description="EchoSnap YOLO 학습"
    )

    parser.add_argument(
        "--epochs",
        type=int,
        default=50,
    )

    parser.add_argument(
        "--imgsz",
        type=int,
        default=640,
    )

    parser.add_argument(
        "--batch",
        type=int,
        default=8,
    )

    parser.add_argument(
        "--name",
        type=str,
        default="echosnap-yolo",
    )

    parser.add_argument(
        "--base-model",
        type=str,
        default="yolo11n.pt",
    )

    parser.add_argument(
        "--device",
        type=str,
        default=None,
    )

    parser.add_argument(
        "--promote",
        action="store_true",
        help="best.pt를 FastAPI용 models/echosnap-yolo.pt로 복사합니다.",
    )

    return parser.parse_args()


def count_images(directory: Path) -> int:
    if not directory.exists():
        return 0

    return sum(
        1
        for path in directory.rglob("*")
        if path.is_file()
        and path.suffix.lower() in IMAGE_EXTENSIONS
    )


def count_labels(directory: Path) -> int:
    if not directory.exists():
        return 0

    return sum(
        1
        for path in directory.rglob("*.txt")
        if path.is_file()
    )


def normalize_names(names) -> list[str]:
    if isinstance(names, list):
        return [
            str(name)
            for name in names
        ]

    if isinstance(names, dict):
        normalized = {
            int(key): str(value)
            for key, value in names.items()
        }

        return [
            normalized[index]
            for index in sorted(normalized)
        ]

    raise ValueError(
        f"지원하지 않는 names 형식입니다: {type(names)}"
    )


def validate_data_yaml() -> None:
    if not DATA_CONFIG_PATH.exists():
        raise FileNotFoundError(
            "병합된 dataset/data.yaml을 찾을 수 없습니다.\n"
            f"경로: {DATA_CONFIG_PATH}"
        )

    with DATA_CONFIG_PATH.open(
        "r",
        encoding="utf-8",
    ) as file:
        config = yaml.safe_load(file)

    names = normalize_names(
        config.get("names")
    )

    if names != EXPECTED_CLASSES:
        raise RuntimeError(
            "dataset/data.yaml의 클래스 순서가 예상과 다릅니다.\n"
            f"예상: {EXPECTED_CLASSES}\n"
            f"실제: {names}"
        )


def validate_dataset() -> None:
    validate_data_yaml()

    required_directories = [
        DATASET_DIR / "images" / "train",
        DATASET_DIR / "images" / "val",
        DATASET_DIR / "labels" / "train",
        DATASET_DIR / "labels" / "val",
    ]

    missing = [
        directory
        for directory in required_directories
        if not directory.exists()
    ]

    if missing:
        missing_text = "\n".join(
            f"- {directory}"
            for directory in missing
        )

        raise FileNotFoundError(
            "필수 데이터셋 폴더가 없습니다.\n"
            f"{missing_text}"
        )

    train_images = count_images(
        DATASET_DIR / "images" / "train"
    )

    train_labels = count_labels(
        DATASET_DIR / "labels" / "train"
    )

    val_images = count_images(
        DATASET_DIR / "images" / "val"
    )

    val_labels = count_labels(
        DATASET_DIR / "labels" / "val"
    )

    print()
    print("Dataset 확인")
    print(
        f"  train images : {train_images}"
    )
    print(
        f"  train labels : {train_labels}"
    )
    print(
        f"  val images   : {val_images}"
    )
    print(
        f"  val labels   : {val_labels}"
    )

    if train_images == 0:
        raise RuntimeError(
            "train 이미지가 없습니다."
        )

    if train_labels == 0:
        raise RuntimeError(
            "train 라벨이 없습니다."
        )

    if val_images == 0:
        raise RuntimeError(
            "val 이미지가 없습니다."
        )

    if val_labels == 0:
        raise RuntimeError(
            "val 라벨이 없습니다."
        )


def resolve_device(requested_device: str | None):
    if requested_device is not None:
        print(
            f"요청된 device 사용: "
            f"{requested_device}"
        )

        return requested_device

    if torch.cuda.is_available():
        gpu_name = torch.cuda.get_device_name(0)

        print("CUDA GPU 사용")
        print(
            f"GPU: {gpu_name}"
        )

        return 0

    print("CUDA GPU를 찾지 못했습니다.")
    print("CPU로 학습합니다.")

    return "cpu"


def promote_model(
        best_model_path: Path,
) -> Path:
    MODELS_DIR.mkdir(
        parents=True,
        exist_ok=True,
    )

    final_model_path = (
        MODELS_DIR
        / "echosnap-yolo.pt"
    )

    shutil.copy2(
        best_model_path,
        final_model_path,
    )

    return final_model_path


def main() -> None:
    args = parse_args()

    print()
    print("=" * 70)
    print("EchoSnap YOLO Training")
    print("=" * 70)

    validate_dataset()

    RUNS_DIR.mkdir(
        parents=True,
        exist_ok=True,
    )

    MODELS_DIR.mkdir(
        parents=True,
        exist_ok=True,
    )

    device = resolve_device(
        args.device
    )

    print()
    print("학습 설정")
    print(
        f"  epochs     : {args.epochs}"
    )
    print(
        f"  image size : {args.imgsz}"
    )
    print(
        f"  batch      : {args.batch}"
    )
    print(
        f"  device     : {device}"
    )
    print(
        f"  base model : {args.base_model}"
    )
    print(
        f"  run name   : {args.name}"
    )
    print()

    model = YOLO(
        args.base_model
    )

    model.train(
        data=str(
            DATA_CONFIG_PATH
        ),
        epochs=args.epochs,
        imgsz=args.imgsz,
        batch=args.batch,
        patience=15,
        device=device,
        workers=0,
        project=str(
            RUNS_DIR
        ),
        name=args.name,
        exist_ok=True,
        pretrained=True,
        verbose=True,
    )

    best_model_path = (
        RUNS_DIR
        / args.name
        / "weights"
        / "best.pt"
    )

    if not best_model_path.exists():
        raise FileNotFoundError(
            "학습은 끝났지만 best.pt를 "
            "찾지 못했습니다.\n"
            f"경로: {best_model_path}"
        )

    print()
    print("=" * 70)
    print("YOLO 학습 완료")
    print("=" * 70)
    print(
        f"best.pt: {best_model_path}"
    )

    if args.promote:
        final_model_path = promote_model(
            best_model_path
        )

        print(
            f"FastAPI 모델: "
            f"{final_model_path}"
        )
    else:
        print()
        print(
            "스모크 테스트이므로 FastAPI 모델로 "
            "승격하지 않았습니다."
        )


if __name__ == "__main__":
    main()