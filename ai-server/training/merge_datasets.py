from __future__ import annotations

import hashlib
import shutil
import zipfile
from collections import defaultdict
from pathlib import Path

import yaml


AI_SERVER_DIR = Path(__file__).resolve().parent.parent

RAW_DATASETS_DIR = AI_SERVER_DIR / "raw-datasets"
WORK_DIR = RAW_DATASETS_DIR / "_merge_work"

OUTPUT_DATASET_DIR = AI_SERVER_DIR / "dataset"


TARGET_CLASSES = [
    "cardboard_box",
    "pet_bottle",
    "plastic_container",
    "can",
    "glass_bottle",
    "styrofoam",
]


TARGET_CLASS_IDS = {
    class_name: class_id
    for class_id, class_name in enumerate(TARGET_CLASSES)
}


# 공개 데이터셋의 실제 클래스명 → EchoSnap 클래스명
SOURCE_CLASS_ALIASES = {
    "cardboard": "cardboard_box",
    "cardboard boxes and cartons": "cardboard_box",

    "plastic bottle": "pet_bottle",

    "plastic container": "plastic_container",

    "can": "can",

    # 첫 데이터셋의 "glass"는 유리 전반이라 일부러 제외.
    # 정확히 "glass bottle"로 라벨링된 것만 사용한다.
    "glass bottle": "glass_bottle",

    "styrofoam": "styrofoam",
}


IMAGE_EXTENSIONS = {
    ".jpg",
    ".jpeg",
    ".png",
    ".bmp",
    ".webp",
}


SPLIT_NAME_MAPPING = {
    "train": "train",
    "training": "train",

    "valid": "val",
    "validation": "val",
    "val": "val",

    "test": "test",
}


def normalize_class_name(value: str) -> str:
    normalized = value.strip().lower()

    normalized = normalized.replace("_", " ")
    normalized = normalized.replace("-", " ")

    return " ".join(normalized.split())


def load_class_names(data_yaml_path: Path) -> dict[int, str]:
    with data_yaml_path.open(
        "r",
        encoding="utf-8",
    ) as file:
        config = yaml.safe_load(file)

    names = config.get("names")

    if names is None:
        raise ValueError(
            f"data.yaml에 names 항목이 없습니다: "
            f"{data_yaml_path}"
        )

    if isinstance(names, list):
        return {
            index: str(name)
            for index, name in enumerate(names)
        }

    if isinstance(names, dict):
        return {
            int(index): str(name)
            for index, name in names.items()
        }

    raise ValueError(
        f"지원하지 않는 names 형식입니다: {type(names)}"
    )


def find_data_yaml(dataset_dir: Path) -> Path:
    candidates = list(
        dataset_dir.rglob("data.yaml")
    )

    if not candidates:
        candidates = list(
            dataset_dir.rglob("*.yaml")
        )

    if not candidates:
        raise FileNotFoundError(
            f"YAML 설정 파일을 찾지 못했습니다: "
            f"{dataset_dir}"
        )

    return candidates[0]


def extract_zip(
        zip_path: Path,
        destination: Path,
) -> None:
    if destination.exists():
        shutil.rmtree(destination)

    destination.mkdir(
        parents=True,
        exist_ok=True,
    )

    with zipfile.ZipFile(
        zip_path,
        "r",
    ) as zip_file:
        zip_file.extractall(destination)


def find_split_directories(
        dataset_root: Path,
) -> list[tuple[str, Path, Path]]:
    discovered = []

    seen = set()

    # Roboflow에서 흔한 구조:
    #
    # train/
    #   images/
    #   labels/
    #
    # valid/
    #   images/
    #   labels/

    for images_dir in dataset_root.rglob("images"):
        parent_name = images_dir.parent.name.lower()

        if parent_name not in SPLIT_NAME_MAPPING:
            continue

        labels_dir = images_dir.parent / "labels"

        if not labels_dir.exists():
            continue

        target_split = SPLIT_NAME_MAPPING[parent_name]

        key = (
            target_split,
            str(images_dir.resolve()),
            str(labels_dir.resolve()),
        )

        if key in seen:
            continue

        seen.add(key)

        discovered.append(
            (
                target_split,
                images_dir,
                labels_dir,
            )
        )

    # 일반 YOLO 구조:
    #
    # images/
    #   train/
    #   val/
    #
    # labels/
    #   train/
    #   val/

    for images_root in dataset_root.rglob("images"):
        labels_root = images_root.parent / "labels"

        if not labels_root.exists():
            continue

        for source_split, target_split in SPLIT_NAME_MAPPING.items():
            images_dir = images_root / source_split
            labels_dir = labels_root / source_split

            if not images_dir.exists():
                continue

            if not labels_dir.exists():
                continue

            key = (
                target_split,
                str(images_dir.resolve()),
                str(labels_dir.resolve()),
            )

            if key in seen:
                continue

            seen.add(key)

            discovered.append(
                (
                    target_split,
                    images_dir,
                    labels_dir,
                )
            )

    return discovered


def calculate_sha256(file_path: Path) -> str:
    sha256 = hashlib.sha256()

    with file_path.open("rb") as file:
        while True:
            chunk = file.read(1024 * 1024)

            if not chunk:
                break

            sha256.update(chunk)

    return sha256.hexdigest()


def read_and_remap_label(
        label_path: Path,
        source_class_names: dict[int, str],
) -> tuple[list[str], set[str]]:
    converted_lines = []

    detected_target_classes = set()

    if not label_path.exists():
        return converted_lines, detected_target_classes

    with label_path.open(
        "r",
        encoding="utf-8",
    ) as file:
        lines = file.readlines()

    for line in lines:
        stripped = line.strip()

        if not stripped:
            continue

        parts = stripped.split()

        if len(parts) < 5:
            continue

        try:
            source_class_id = int(float(parts[0]))
        except ValueError:
            continue

        source_class_name = source_class_names.get(
            source_class_id
        )

        if source_class_name is None:
            continue

        normalized_source_name = normalize_class_name(
            source_class_name
        )

        target_class_name = SOURCE_CLASS_ALIASES.get(
            normalized_source_name
        )

        if target_class_name is None:
            continue

        target_class_id = TARGET_CLASS_IDS[
            target_class_name
        ]

        converted_line = " ".join(
            [
                str(target_class_id),
                *parts[1:],
            ]
        )

        converted_lines.append(
            converted_line
        )

        detected_target_classes.add(
            target_class_name
        )

    return (
        converted_lines,
        detected_target_classes,
    )


def prepare_output_directories() -> None:
    if OUTPUT_DATASET_DIR.exists():
        shutil.rmtree(
            OUTPUT_DATASET_DIR
        )

    for split in [
        "train",
        "val",
        "test",
    ]:
        (
            OUTPUT_DATASET_DIR
            / "images"
            / split
        ).mkdir(
            parents=True,
            exist_ok=True,
        )

        (
            OUTPUT_DATASET_DIR
            / "labels"
            / split
        ).mkdir(
            parents=True,
            exist_ok=True,
        )


def write_classes_file() -> None:
    classes_path = (
        OUTPUT_DATASET_DIR
        / "classes.txt"
    )

    classes_path.write_text(
        "\n".join(TARGET_CLASSES) + "\n",
        encoding="utf-8",
    )


def write_data_yaml() -> None:
    data_yaml_path = (
        OUTPUT_DATASET_DIR
        / "data.yaml"
    )

    config = {
        "path": str(
            OUTPUT_DATASET_DIR.resolve()
        ),
        "train": "images/train",
        "val": "images/val",
        "test": "images/test",
        "names": {
            index: class_name
            for index, class_name
            in enumerate(TARGET_CLASSES)
        },
    }

    with data_yaml_path.open(
        "w",
        encoding="utf-8",
    ) as file:
        yaml.safe_dump(
            config,
            file,
            allow_unicode=True,
            sort_keys=False,
        )


def process_split(
        source_index: int,
        split: str,
        images_dir: Path,
        labels_dir: Path,
        source_class_names: dict[int, str],
        seen_image_hashes: set[str],
        image_counts: dict,
        annotation_counts: dict,
) -> tuple[int, int]:
    copied_images = 0
    skipped_duplicates = 0

    image_paths = sorted(
        path
        for path in images_dir.rglob("*")
        if path.is_file()
        and path.suffix.lower() in IMAGE_EXTENSIONS
    )

    for image_path in image_paths:
        relative_image_path = image_path.relative_to(
            images_dir
        )

        label_path = (
            labels_dir
            / relative_image_path
        ).with_suffix(".txt")

        (
            converted_labels,
            contained_classes,
        ) = read_and_remap_label(
            label_path=label_path,
            source_class_names=source_class_names,
        )

        # 우리가 사용하는 객체가 없는 이미지는 제외한다.
        if not converted_labels:
            continue

        image_hash = calculate_sha256(
            image_path
        )

        # 두 공개 데이터셋에 같은 이미지가 들어있는 경우
        # train / val에 중복으로 들어가는 것을 막는다.
        if image_hash in seen_image_hashes:
            skipped_duplicates += 1
            continue

        seen_image_hashes.add(
            image_hash
        )

        short_hash = image_hash[:12]

        destination_stem = (
            f"src{source_index}_"
            f"{short_hash}"
        )

        destination_image_path = (
            OUTPUT_DATASET_DIR
            / "images"
            / split
            / (
                destination_stem
                + image_path.suffix.lower()
            )
        )

        destination_label_path = (
            OUTPUT_DATASET_DIR
            / "labels"
            / split
            / (
                destination_stem
                + ".txt"
            )
        )

        shutil.copy2(
            image_path,
            destination_image_path,
        )

        destination_label_path.write_text(
            "\n".join(converted_labels)
            + "\n",
            encoding="utf-8",
        )

        copied_images += 1

        for target_class_name in contained_classes:
            image_counts[
                split
            ][
                target_class_name
            ] += 1

        for converted_label in converted_labels:
            target_class_id = int(
                converted_label.split()[0]
            )

            target_class_name = (
                TARGET_CLASSES[
                    target_class_id
                ]
            )

            annotation_counts[
                split
            ][
                target_class_name
            ] += 1

    return (
        copied_images,
        skipped_duplicates,
    )


def print_source_classes(
        zip_name: str,
        source_class_names: dict[int, str],
) -> None:
    print()
    print(
        f"[{zip_name}] 원본 클래스"
    )

    for class_id, class_name in sorted(
            source_class_names.items()
    ):
        normalized = normalize_class_name(
            class_name
        )

        target = SOURCE_CLASS_ALIASES.get(
            normalized
        )

        if target is None:
            marker = "-"
        else:
            marker = f"→ {target}"

        print(
            f"  {class_id:>2} "
            f"{class_name:<30} "
            f"{marker}"
        )


def print_summary(
        image_counts: dict,
        annotation_counts: dict,
        total_images: dict,
        duplicate_count: int,
) -> None:
    print()
    print("=" * 80)
    print("EchoSnap 데이터셋 병합 결과")
    print("=" * 80)

    for split in [
        "train",
        "val",
        "test",
    ]:
        print()
        print(
            f"[{split}] "
            f"선택된 이미지: "
            f"{total_images.get(split, 0)}"
        )

        for class_name in TARGET_CLASSES:
            images = image_counts[
                split
            ].get(
                class_name,
                0,
            )

            annotations = annotation_counts[
                split
            ].get(
                class_name,
                0,
            )

            print(
                f"  {class_name:<20} "
                f"images={images:<6} "
                f"objects={annotations}"
            )

    print()
    print(
        f"중복 이미지 제외: "
        f"{duplicate_count}"
    )


def validate_final_dataset(
        annotation_counts: dict,
) -> None:
    total_by_class = {
        class_name: 0
        for class_name in TARGET_CLASSES
    }

    for split_counts in annotation_counts.values():
        for class_name, count in split_counts.items():
            total_by_class[
                class_name
            ] += count

    missing_classes = [
        class_name
        for class_name, count
        in total_by_class.items()
        if count == 0
    ]

    print()
    print("전체 클래스 객체 수")

    for class_name in TARGET_CLASSES:
        print(
            f"  {class_name:<20} "
            f"{total_by_class[class_name]}"
        )

    if missing_classes:
        raise RuntimeError(
            "다음 EchoSnap 클래스의 "
            "라벨을 찾지 못했습니다: "
            + ", ".join(missing_classes)
        )


def main() -> None:
    print()
    print(
        "EchoSnap 공개 데이터셋 "
        "6종 자동 병합"
    )
    print()

    zip_files = sorted(
        RAW_DATASETS_DIR.glob("*.zip")
    )

    if not zip_files:
        raise FileNotFoundError(
            f"ZIP 파일이 없습니다: "
            f"{RAW_DATASETS_DIR}"
        )

    print(
        f"발견된 ZIP: "
        f"{len(zip_files)}개"
    )

    prepare_output_directories()

    if WORK_DIR.exists():
        shutil.rmtree(
            WORK_DIR
        )

    WORK_DIR.mkdir(
        parents=True,
        exist_ok=True,
    )

    seen_image_hashes = set()

    image_counts = defaultdict(
        lambda: defaultdict(int)
    )

    annotation_counts = defaultdict(
        lambda: defaultdict(int)
    )

    total_images = defaultdict(int)

    total_duplicate_count = 0

    for source_index, zip_path in enumerate(
            zip_files,
            start=1,
    ):
        print()
        print("=" * 80)
        print(
            f"데이터셋 {source_index}: "
            f"{zip_path.name}"
        )
        print("=" * 80)

        extraction_dir = (
            WORK_DIR
            / f"source_{source_index}"
        )

        extract_zip(
            zip_path,
            extraction_dir,
        )

        data_yaml_path = find_data_yaml(
            extraction_dir
        )

        source_class_names = load_class_names(
            data_yaml_path
        )

        print_source_classes(
            zip_path.name,
            source_class_names,
        )

        split_directories = (
            find_split_directories(
                extraction_dir
            )
        )

        if not split_directories:
            raise RuntimeError(
                f"train/valid/test 폴더를 "
                f"찾지 못했습니다: "
                f"{zip_path.name}"
            )

        for (
            target_split,
            images_dir,
            labels_dir,
        ) in split_directories:
            print()
            print(
                f"처리 중: "
                f"{zip_path.name} "
                f"→ {target_split}"
            )

            (
                copied,
                duplicates,
            ) = process_split(
                source_index=source_index,
                split=target_split,
                images_dir=images_dir,
                labels_dir=labels_dir,
                source_class_names=source_class_names,
                seen_image_hashes=seen_image_hashes,
                image_counts=image_counts,
                annotation_counts=annotation_counts,
            )

            total_images[
                target_split
            ] += copied

            total_duplicate_count += (
                duplicates
            )

            print(
                f"  복사: {copied}"
            )

            print(
                f"  중복 제외: {duplicates}"
            )

    write_classes_file()
    write_data_yaml()

    print_summary(
        image_counts=image_counts,
        annotation_counts=annotation_counts,
        total_images=total_images,
        duplicate_count=total_duplicate_count,
    )

    validate_final_dataset(
        annotation_counts
    )

    if WORK_DIR.exists():
        shutil.rmtree(
            WORK_DIR
        )

    print()
    print("=" * 80)
    print("병합 완료")
    print("=" * 80)

    print(
        f"Dataset: "
        f"{OUTPUT_DATASET_DIR}"
    )

    print(
        f"Config : "
        f"{OUTPUT_DATASET_DIR / 'data.yaml'}"
    )

    print()
    print(
        "다음 단계에서 클래스별 데이터 수를 "
        "확인한 후 YOLO 학습을 시작합니다."
    )


if __name__ == "__main__":
    main()