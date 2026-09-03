import 'dart:typed_data';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:image/image.dart'
as img;
import 'package:tflite_flutter/tflite_flutter.dart';

/// EchoSnap 모바일 TFLite 분석 결과입니다.
///
/// 현재 모델은 YOLO Object Detection 모델이지만
/// EchoSnap 사용자 흐름에서는
/// 이미지에서 가장 신뢰도가 높은 폐기물 품목 하나를
/// 1차 결과로 사용합니다.
///
/// 실제 배출 가능 여부를 확정하는 것이 아니라
/// "AI 추정 결과"로 사용합니다.
class TfliteWasteDetectionResult {
  const TfliteWasteDetectionResult({
    required this.classId,
    required this.modelLabel,
    required this.confidence,
    required this.modelVersion,
  });

  /// YOLO 모델 내부 class index입니다.
  ///
  /// 주의:
  /// WasteItem DB ID와는 전혀 다른 값입니다.
  final int classId;

  /// 실제 모델 label입니다.
  ///
  /// 예:
  /// plastic_container
  final String modelLabel;

  /// AI 신뢰도
  ///
  /// 0.0 ~ 1.0
  final double confidence;

  /// 앱에 포함된 TFLite 모델 버전입니다.
  final String modelVersion;
}

class TfliteWasteDetectorException
    implements Exception {
  const TfliteWasteDetectorException(
      this.message,
      );

  final String message;

  @override
  String toString() {
    return message;
  }
}

/// Flutter 기기 안에서 실행되는
/// EchoSnap YOLO TFLite 분석기입니다.
///
/// 실제 모델 구조:
///
/// INPUT
/// [1, 3, 640, 640]
/// float32
///
/// OUTPUT
/// [1, 10, 8400]
/// float32
///
/// OUTPUT의 10개 channel:
///
/// 0 = x
/// 1 = y
/// 2 = width
/// 3 = height
///
/// 4 = cardboard_box score
/// 5 = pet_bottle score
/// 6 = plastic_container score
/// 7 = can score
/// 8 = glass_bottle score
/// 9 = styrofoam score
class TfliteWasteDetector {
  static const String _modelAsset =
      'assets/models/'
      'echosnap-yolo.tflite';

  static const String _labelsAsset =
      'assets/models/labels.txt';

  /// Spring Boot ImageLog에도 저장되는
  /// 모바일 모델 버전입니다.
  static const String modelVersion =
      'echosnap-tflite-v1';

  static const int _inputWidth = 640;
  static const int _inputHeight = 640;

  static const int _inputChannels = 3;

  static const int _classCount = 6;

  static const int _outputChannels =
      4 + _classCount;

  static const int _predictionCount =
  8400;

  static const List<int> _expectedInputShape =
  <int>[
    1,
    3,
    640,
    640,
  ];

  static const List<int> _expectedOutputShape =
  <int>[
    1,
    10,
    8400,
  ];

  Interpreter? _interpreter;

  List<String>? _labels;

  /// 모델이 실제로 메모리에 올라와 있는지
  /// 확인할 때 사용합니다.
  bool get isLoaded {
    return _interpreter != null &&
        _labels != null;
  }

  /// TFLite 모델과 labels 파일을 로드합니다.
  ///
  /// analyze()에서도 자동 호출되므로
  /// 화면 진입 시 반드시 직접 호출할 필요는 없습니다.
  Future<void> initialize() async {
    if (isLoaded) {
      return;
    }

    if (kIsWeb) {
      throw const TfliteWasteDetectorException(
        '현재 EchoSnap TFLite 분석은 '
            'Android 실기기에서 사용합니다.',
      );
    }

    try {
      final Interpreter interpreter =
      await Interpreter.fromAsset(
        _modelAsset,
      );

      final List<String> labels =
      await _loadLabels();

      _validateModel(
        interpreter,
        labels,
      );

      _interpreter =
          interpreter;

      _labels =
          labels;
    } on TfliteWasteDetectorException {
      rethrow;
    } catch (exception) {
      throw TfliteWasteDetectorException(
        'TFLite 모델을 불러오지 못했습니다. '
            '$exception',
      );
    }
  }

  /// 이미지 byte를 실제 YOLO TFLite 모델로 분석합니다.
  Future<TfliteWasteDetectionResult>
  analyze({
    required Uint8List imageBytes,
  }) async {
    if (imageBytes.isEmpty) {
      throw const TfliteWasteDetectorException(
        '분석할 이미지가 비어 있습니다.',
      );
    }

    await initialize();

    final Interpreter interpreter =
    _interpreter!;

    final List<String> labels =
    _labels!;

    /*
     * =======================================================
     * 1. 이미지 Decode
     * =======================================================
     */

    final img.Image? decodedImage =
    img.decodeImage(
      imageBytes,
    );

    if (decodedImage == null) {
      throw const TfliteWasteDetectorException(
        'AI 분석용 이미지를 읽지 못했습니다.',
      );
    }

    /*
     * =======================================================
     * 2. YOLO 입력 크기 640 x 640으로 변환
     * =======================================================
     *
     * 단순히 이미지를 찌그러뜨려
     * 640 x 640으로 만드는 대신,
     *
     * 원본 종횡비를 유지하고
     * 남는 영역을 RGB(114, 114, 114)로 채웁니다.
     *
     * YOLO에서 일반적으로 사용하는
     * letterbox 계열 전처리입니다.
     */

    final img.Image resizedImage =
    img.copyResize(
      decodedImage,
      width: _inputWidth,
      height: _inputHeight,
      maintainAspect: true,
      backgroundColor:
      img.ColorRgb8(
        114,
        114,
        114,
      ),
      interpolation:
      img.Interpolation.linear,
    );

    /*
     * =======================================================
     * 3. RGB 이미지 -> Float32 NCHW 변환
     * =======================================================
     *
     * 실제 모델 입력:
     *
     * [1, 3, 640, 640]
     *
     * 즉,
     *
     * N = Batch
     * C = RGB Channel
     * H = Height
     * W = Width
     *
     * 순서입니다.
     *
     * 픽셀 값은
     *
     * 0 ~ 255
     *
     * 에서
     *
     * 0.0 ~ 1.0
     *
     * 으로 정규화합니다.
     */

    final Float32List input =
    _createInputTensor(
      resizedImage,
    );

    /*
     * tflite_flutter에 중첩 List를 그대로 전달하면
     * Dart 객체가 매우 많이 생성될 수 있습니다.
     *
     * 따라서 Float32List의 실제 byte buffer를
     * TFLite Tensor에 직접 전달합니다.
     *
     * 모델 자체의 입력 shape는 이미
     * [1, 3, 640, 640]으로 고정되어 있습니다.
     */
    final Uint8List inputBytes =
    input.buffer.asUint8List(
      input.offsetInBytes,
      input.lengthInBytes,
    );

    /*
     * OUTPUT:
     *
     * 1 * 10 * 8400개의 float32
     */
    final int outputElementCount =
        _outputChannels *
            _predictionCount;

    final Uint8List outputBytes =
    Uint8List(
      outputElementCount *
          Float32List.bytesPerElement,
    );

    /*
     * =======================================================
     * 4. 실제 TensorFlow Lite 추론
     * =======================================================
     */

    try {
      interpreter.run(
        inputBytes,
        outputBytes,
      );
    } catch (exception) {
      throw TfliteWasteDetectorException(
        '기기 내 AI 분석 중 오류가 발생했습니다. '
            '$exception',
      );
    }

    final Float32List output =
    outputBytes.buffer
        .asFloat32List();

    /*
     * =======================================================
     * 5. YOLO 결과 해석
     * =======================================================
     */

    return _parseOutput(
      output,
      labels,
    );
  }

  /// 이미지 픽셀을 NCHW Float32 배열로 변환합니다.
  Float32List _createInputTensor(
      img.Image image,
      ) {
    if (image.width != _inputWidth ||
        image.height != _inputHeight) {
      throw const TfliteWasteDetectorException(
        'AI 입력 이미지 크기가 올바르지 않습니다.',
      );
    }

    final int planeSize =
        _inputWidth *
            _inputHeight;

    final Float32List input =
    Float32List(
      _inputChannels *
          planeSize,
    );

    /*
     * NCHW 구조:
     *
     * [RRRRR...][GGGGG...][BBBBB...]
     */
    final int redOffset = 0;

    final int greenOffset =
        planeSize;

    final int blueOffset =
        planeSize * 2;

    for (
    int y = 0;
    y < _inputHeight;
    y++
    ) {
      for (
      int x = 0;
      x < _inputWidth;
      x++
      ) {
        final img.Pixel pixel =
        image.getPixel(
          x,
          y,
        );

        final int pixelIndex =
            y * _inputWidth + x;

        input[
        redOffset +
            pixelIndex] =
            pixel.r.toDouble() /
                255.0;

        input[
        greenOffset +
            pixelIndex] =
            pixel.g.toDouble() /
                255.0;

        input[
        blueOffset +
            pixelIndex] =
            pixel.b.toDouble() /
                255.0;
      }
    }

    return input;
  }

  /// YOLO Detection output에서
  /// 가장 높은 confidence를 가진 품목을 선택합니다.
  ///
  /// 현재 EchoSnap의 모바일 1차 분석 목적은
  /// Bounding Box 자체를 사용자에게 그리는 것이 아니라
  /// "이 폐기물이 어떤 품목으로 추정되는가"를
  /// 빠르게 판단하는 것입니다.
  ///
  /// 따라서 모든 8,400개 후보 중
  /// 가장 높은 class score 하나를 선택합니다.
  ///
  /// 이후 낮은 confidence라면
  /// Spring Boot가
  /// SERVER_REANALYSIS_PENDING으로 변경하고
  /// Python YOLO가 다시 분석합니다.
  TfliteWasteDetectionResult _parseOutput(
      Float32List output,
      List<String> labels,
      ) {
    final int expectedLength =
        _outputChannels *
            _predictionCount;

    if (output.length !=
        expectedLength) {
      throw TfliteWasteDetectorException(
        'AI 출력 크기가 예상과 다릅니다. '
            'expected=$expectedLength, '
            'actual=${output.length}',
      );
    }

    int bestClassId = 0;

    double bestConfidence =
        double.negativeInfinity;

    /*
     * output shape:
     *
     * [1, 10, 8400]
     *
     * 평탄화하면:
     *
     * channel 0의 8400개 값
     * channel 1의 8400개 값
     * ...
     * channel 9의 8400개 값
     *
     * 순서입니다.
     *
     * channel 0~3:
     * Bounding Box
     *
     * channel 4~9:
     * 6개 class score
     */

    for (
    int predictionIndex = 0;
    predictionIndex <
        _predictionCount;
    predictionIndex++
    ) {
      for (
      int classId = 0;
      classId < _classCount;
      classId++
      ) {
        final int channel =
            4 + classId;

        final int outputIndex =
            channel *
                _predictionCount +
                predictionIndex;

        final double confidence =
        output[
        outputIndex
        ];

        if (confidence.isNaN ||
            confidence.isInfinite) {
          continue;
        }

        if (confidence >
            bestConfidence) {
          bestConfidence =
              confidence;

          bestClassId =
              classId;
        }
      }
    }

    /*
     * 정상적인 YOLO class score는
     * 0.0 ~ 1.0 범위입니다.
     *
     * 혹시 모델/런타임 오차로 범위를 벗어난 경우
     * 사용자와 서버에는 정상 범위로 전달합니다.
     */
    final double normalizedConfidence =
    bestConfidence
        .clamp(
      0.0,
      1.0,
    )
        .toDouble();

    return TfliteWasteDetectionResult(
      classId:
      bestClassId,

      modelLabel:
      labels[
      bestClassId
      ],

      confidence:
      normalizedConfidence,

      modelVersion:
      modelVersion,
    );
  }

  /// labels.txt를 읽습니다.
  Future<List<String>>
  _loadLabels() async {
    final String rawLabels =
    await rootBundle.loadString(
      _labelsAsset,
    );

    final List<String> labels =
    rawLabels
        .split(
      RegExp(
        r'\r?\n',
      ),
    )
        .map(
          (label) =>
          label.trim(),
    )
        .where(
          (label) =>
      label.isNotEmpty,
    )
        .toList(
      growable: false,
    );

    /*
     * UTF-8 BOM이 붙어있는 txt인 경우를 대비합니다.
     */
    if (labels.isNotEmpty) {
      labels[0] =
          labels[0]
              .replaceFirst(
            '\uFEFF',
            '',
          );
    }

    return labels;
  }

  /// 앱에 포함된 모델이 우리가 확인했던
  /// 실제 EchoSnap 모델과 동일한 구조인지 검증합니다.
  ///
  /// 모델 파일을 잘못 교체했을 때
  /// 조용히 틀린 결과가 나오는 것을 방지합니다.
  void _validateModel(
      Interpreter interpreter,
      List<String> labels,
      ) {
    if (labels.length !=
        _classCount) {
      throw TfliteWasteDetectorException(
        'AI 라벨 개수가 올바르지 않습니다. '
            'expected=$_classCount, '
            'actual=${labels.length}',
      );
    }

    final Tensor inputTensor =
    interpreter
        .getInputTensor(
      0,
    );

    final Tensor outputTensor =
    interpreter
        .getOutputTensor(
      0,
    );

    if (!_sameShape(
      inputTensor.shape,
      _expectedInputShape,
    )) {
      throw TfliteWasteDetectorException(
        'TFLite 입력 Tensor 구조가 '
            '예상과 다릅니다. '
            'expected=$_expectedInputShape, '
            'actual=${inputTensor.shape}',
      );
    }

    if (!_sameShape(
      outputTensor.shape,
      _expectedOutputShape,
    )) {
      throw TfliteWasteDetectorException(
        'TFLite 출력 Tensor 구조가 '
            '예상과 다릅니다. '
            'expected=$_expectedOutputShape, '
            'actual=${outputTensor.shape}',
      );
    }

    if (inputTensor.type !=
        TensorType.float32) {
      throw TfliteWasteDetectorException(
        'TFLite 입력 자료형이 '
            'float32가 아닙니다. '
            'actual=${inputTensor.type}',
      );
    }

    if (outputTensor.type !=
        TensorType.float32) {
      throw TfliteWasteDetectorException(
        'TFLite 출력 자료형이 '
            'float32가 아닙니다. '
            'actual=${outputTensor.type}',
      );
    }
  }

  bool _sameShape(
      List<int> actual,
      List<int> expected,
      ) {
    if (actual.length !=
        expected.length) {
      return false;
    }

    for (
    int index = 0;
    index < actual.length;
    index++
    ) {
      if (actual[index] !=
          expected[index]) {
        return false;
      }
    }

    return true;
  }

  /// 화면이 완전히 종료될 때
  /// native TFLite Interpreter를 정리할 수 있습니다.
  void close() {
    _interpreter?.close();

    _interpreter = null;

    _labels = null;
  }
}