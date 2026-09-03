from __future__ import annotations

from pathlib import Path


AI_SERVER_DIR = Path(__file__).resolve().parent.parent
DATASET_DIR = AI_SERVER_DIR / "dataset"
LABELS_DIR = DATASET_DIR / "labels"

SPLITS = [
    "train",
    "val",
    "test",
]


def clamp(value: float) -> float:
    return max(0.0, min(1.0, value))


def polygon_to_bbox(
    coordinates: list[float],
) -> tuple[float, float, float, float]:
    if len(coordinates) < 6:
        raise ValueError(
            "Polygon 좌표는 최소 3개의 점이 필요합니다."
        )

    if len(coordinates) % 2 != 0:
        raise ValueError(
            "Polygon 좌표의 x/y 개수가 맞지 않습니다."
        )

    xs = coordinates[0::2]
    ys = coordinates[1::2]

    min_x = clamp(min(xs))
    max_x = clamp(max(xs))
    min_y = clamp(min(ys))
    max_y = clamp(max(ys))

    width = max_x - min_x
    height = max_y - min_y

    center_x = min_x + width / 2
    center_y = min_y + height / 2

    return (
        center_x,
        center_y,
        width,
        height,
    )


def normalize_line(
    line: str,
) -> tuple[str | None, bool]:
    stripped = line.strip()

    if not stripped:
        return None, False

    parts = stripped.split()

    if len(parts) < 5:
        return None, False

    try:
        class_id = int(float(parts[0]))
        coordinates = [
            float(value)
            for value in parts[1:]
        ]
    except ValueError:
        return None, False

    # 일반 YOLO Detection
    #
    # class_id center_x center_y width height
    if len(coordinates) == 4:
        center_x, center_y, width, height = coordinates

        normalized = (
            f"{class_id} "
            f"{clamp(center_x):.6f} "
            f"{clamp(center_y):.6f} "
            f"{clamp(width):.6f} "
            f"{clamp(height):.6f}"
        )

        return normalized, False

    # YOLO Segmentation
    #
    # class_id x1 y1 x2 y2 x3 y3 ...
    center_x, center_y, width, height = polygon_to_bbox(
        coordinates
    )

    if width <= 0 or height <= 0:
        return None, True

    normalized = (
        f"{class_id} "
        f"{center_x:.6f} "
        f"{center_y:.6f} "
        f"{width:.6f} "
        f"{height:.6f}"
    )

    return normalized, True


def normalize_label_file(
    label_path: Path,
) -> tuple[int, int, int]:
    original_lines = label_path.read_text(
        encoding="utf-8"
    ).splitlines()

    output_lines = []

    detection_count = 0
    converted_segment_count = 0
    invalid_count = 0

    for line in original_lines:
        try:
            normalized_line, converted = normalize_line(
                line
            )
        except ValueError:
            invalid_count += 1
            continue

        if normalized_line is None:
            invalid_count += 1
            continue

        output_lines.append(
            normalized_line
        )

        if converted:
            converted_segment_count += 1
        else:
            detection_count += 1

    label_path.write_text(
        (
            "\n".join(output_lines) + "\n"
            if output_lines
            else ""
        ),
        encoding="utf-8",
    )

    return (
        detection_count,
        converted_segment_count,
        invalid_count,
    )


def remove_ultralytics_cache() -> None:
    cache_files = list(
        LABELS_DIR.rglob("*.cache")
    )

    for cache_file in cache_files:
        cache_file.unlink()

        print(
            f"캐시 삭제: {cache_file}"
        )


def main() -> None:
    print()
    print("=" * 70)
    print("EchoSnap Detection Label Normalizer")
    print("=" * 70)

    if not LABELS_DIR.exists():
        raise FileNotFoundError(
            f"labels 폴더가 없습니다: {LABELS_DIR}"
        )

    total_files = 0
    total_detection = 0
    total_converted = 0
    total_invalid = 0

    for split in SPLITS:
        split_dir = LABELS_DIR / split

        if not split_dir.exists():
            continue

        label_files = sorted(
            split_dir.glob("*.txt")
        )

        split_detection = 0
        split_converted = 0
        split_invalid = 0

        for label_path in label_files:
            (
                detection_count,
                converted_count,
                invalid_count,
            ) = normalize_label_file(
                label_path
            )

            split_detection += detection_count
            split_converted += converted_count
            split_invalid += invalid_count

        total_files += len(label_files)
        total_detection += split_detection
        total_converted += split_converted
        total_invalid += split_invalid

        print()
        print(f"[{split}]")
        print(
            f"  label files       : {len(label_files)}"
        )
        print(
            f"  기존 bbox         : {split_detection}"
        )
        print(
            f"  polygon → bbox    : {split_converted}"
        )
        print(
            f"  제외된 잘못된 라벨: {split_invalid}"
        )

    remove_ultralytics_cache()

    print()
    print("=" * 70)
    print("라벨 정규화 완료")
    print("=" * 70)

    print(
        f"처리한 label files : {total_files}"
    )

    print(
        f"기존 bbox           : {total_detection}"
    )

    print(
        f"polygon → bbox      : {total_converted}"
    )

    print(
        f"제외된 라벨         : {total_invalid}"
    )

    print()
    print(
        "이제 데이터셋은 Object Detection "
        "Bounding Box 형식으로 통일되었습니다."
    )


if __name__ == "__main__":
    main()