from pathlib import Path
import shutil
import zipfile

import yaml


AI_SERVER_DIR = Path(__file__).resolve().parent.parent
RAW_DATASETS_DIR = AI_SERVER_DIR / "raw-datasets"
EXTRACTED_DIR = RAW_DATASETS_DIR / "extracted"

IMAGE_EXTENSIONS = {
    ".jpg",
    ".jpeg",
    ".png",
    ".bmp",
    ".webp",
}


def find_data_yaml(dataset_dir: Path) -> Path:
    candidates = list(dataset_dir.rglob("data.yaml"))

    if not candidates:
        raise FileNotFoundError(
            f"data.yaml을 찾을 수 없습니다: {dataset_dir}"
        )

    if len(candidates) > 1:
        print("  [주의] data.yaml이 여러 개 발견되었습니다.")
        for candidate in candidates:
            print(f"    - {candidate}")

    return candidates[0]


def normalize_names(names):
    if isinstance(names, dict):
        normalized = {}

        for key, value in names.items():
            normalized[int(key)] = str(value)

        return dict(sorted(normalized.items()))

    if isinstance(names, list):
        return {
            index: str(value)
            for index, value in enumerate(names)
        }

    raise ValueError(
        f"지원하지 않는 names 형식입니다: {type(names)}"
    )


def count_files(directory: Path, extensions=None) -> int:
    if not directory.exists():
        return 0

    if extensions is None:
        return sum(
            1
            for path in directory.rglob("*")
            if path.is_file()
        )

    return sum(
        1
        for path in directory.rglob("*")
        if path.is_file()
        and path.suffix.lower() in extensions
    )


def print_split_statistics(dataset_root: Path) -> None:
    split_candidates = [
        "train",
        "valid",
        "val",
        "test",
    ]

    print()
    print("  데이터 개수")

    found_split = False

    for split in split_candidates:
        images_dir = dataset_root / split / "images"
        labels_dir = dataset_root / split / "labels"

        # Roboflow YOLO 형식 1
        if images_dir.exists() or labels_dir.exists():
            found_split = True

            image_count = count_files(
                images_dir,
                IMAGE_EXTENSIONS,
            )

            label_count = count_files(
                labels_dir,
                {".txt"},
            )

            print(
                f"    {split:<5} "
                f"images={image_count:<6} "
                f"labels={label_count}"
            )

    # 일반 YOLO 형식 2
    images_root = dataset_root / "images"
    labels_root = dataset_root / "labels"

    if images_root.exists() or labels_root.exists():
        for split in split_candidates:
            images_dir = images_root / split
            labels_dir = labels_root / split

            if not images_dir.exists() and not labels_dir.exists():
                continue

            found_split = True

            image_count = count_files(
                images_dir,
                IMAGE_EXTENSIONS,
            )

            label_count = count_files(
                labels_dir,
                {".txt"},
            )

            print(
                f"    {split:<5} "
                f"images={image_count:<6} "
                f"labels={label_count}"
            )

    if not found_split:
        print("    일반적인 YOLO split 폴더를 찾지 못했습니다.")


def inspect_dataset(zip_path: Path) -> None:
    dataset_name = zip_path.stem

    destination = EXTRACTED_DIR / dataset_name

    if destination.exists():
        shutil.rmtree(destination)

    destination.mkdir(
        parents=True,
        exist_ok=True,
    )

    print()
    print("=" * 70)
    print(f"ZIP: {zip_path.name}")
    print("=" * 70)

    with zipfile.ZipFile(zip_path, "r") as zip_file:
        zip_file.extractall(destination)

    data_yaml_path = find_data_yaml(destination)

    print(f"data.yaml: {data_yaml_path}")

    with data_yaml_path.open(
        "r",
        encoding="utf-8",
    ) as file:
        config = yaml.safe_load(file)

    names = config.get("names")

    if names is None:
        raise ValueError(
            f"data.yaml에 names가 없습니다: {data_yaml_path}"
        )

    normalized_names = normalize_names(names)

    print()
    print(f"클래스 수: {len(normalized_names)}")
    print()

    for class_id, class_name in normalized_names.items():
        print(
            f"  {class_id:>2} -> {class_name}"
        )

    dataset_root = data_yaml_path.parent

    print_split_statistics(dataset_root)


def main() -> None:
    print()
    print("EchoSnap 공개 데이터셋 검사")
    print()

    if not RAW_DATASETS_DIR.exists():
        raise FileNotFoundError(
            f"raw-datasets 폴더가 없습니다: "
            f"{RAW_DATASETS_DIR}"
        )

    zip_files = sorted(
        RAW_DATASETS_DIR.glob("*.zip")
    )

    if not zip_files:
        raise FileNotFoundError(
            "raw-datasets 폴더에 ZIP 파일이 없습니다."
        )

    print(
        f"발견된 ZIP 파일: {len(zip_files)}개"
    )

    for zip_path in zip_files:
        inspect_dataset(zip_path)

    print()
    print("=" * 70)
    print("검사 완료")
    print("=" * 70)
    print()
    print(
        "위 클래스 목록을 확인한 뒤 "
        "EchoSnap 6개 클래스로 변환합니다."
    )


if __name__ == "__main__":
    main()