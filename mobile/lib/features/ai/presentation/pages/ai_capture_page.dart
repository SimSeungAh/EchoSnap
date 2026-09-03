import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import 'package:echosnap/app/app_routes.dart';
import 'package:echosnap/core/storage/token_storage.dart';
import 'package:echosnap/core/theme/app_theme.dart';
import 'package:echosnap/features/ai/data/image_correction_api.dart';
import 'package:echosnap/features/ai/data/image_upload_api.dart';
import 'package:echosnap/features/ai/data/mobile_analysis_api.dart';
import 'package:echosnap/features/ai/data/server_reanalysis_api.dart';
import 'package:echosnap/features/ai/data/tflite_waste_detector.dart';
import 'package:echosnap/features/ai/presentation/widgets/ai_analysis_result_card.dart';
import 'package:echosnap/features/waste/data/waste_search_api.dart';
import 'package:echosnap/features/waste/presentation/pages/waste_search_page.dart';

class AiCapturePage
    extends StatefulWidget {
  const AiCapturePage({
    super.key,
  });

  @override
  State<AiCapturePage> createState() =>
      _AiCapturePageState();
}

class _AiCapturePageState
    extends State<AiCapturePage> {
  final ImagePicker _imagePicker =
  ImagePicker();

  final TfliteWasteDetector
  _tfliteDetector =
  TfliteWasteDetector();

  XFile? _selectedImage;

  Uint8List? _selectedImageBytes;

  _FinalAiResult? _finalResult;

  /*
   * AI가 최종 결과를 확정하지 못했더라도
   * 모바일 AI 결과가 서버에 저장된 경우
   * 사용자의 직접 선택을 정정 데이터로
   * 남길 수 있도록 ImageLog 정보를 보존합니다.
   */
  int? _latestImageLogId;

  bool _hasRecordedAiResult = false;

  bool _isPicking = false;

  bool _isAnalyzing = false;

  bool _isCorrecting = false;

  bool _correctionSubmitted = false;

  bool _usedServerReanalysis =
  false;

  String? _progressMessage;

  String? _analysisError;

  bool get _isBusy =>
      _isPicking ||
          _isAnalyzing ||
          _isCorrecting;

  @override
  void dispose() {
    _tfliteDetector.close();

    super.dispose();
  }

  Future<void> _takePhoto() async {
    if (kIsWeb) {
      _showMessage(
        '실제 AI 촬영 분석은 '
            'Android 기기에서 이용해주세요.',
      );

      return;
    }

    await _pickImage(
      ImageSource.camera,
    );
  }

  Future<void>
  _pickFromGallery() async {
    await _pickImage(
      ImageSource.gallery,
    );
  }

  Future<void> _pickImage(
      ImageSource source,
      ) async {
    if (_isBusy) {
      return;
    }

    setState(() {
      _isPicking = true;
    });

    try {
      final XFile? image =
      await _imagePicker.pickImage(
        source: source,
        imageQuality: 90,
        maxWidth: 1600,
      );

      if (image == null) {
        return;
      }

      final Uint8List bytes =
      await image.readAsBytes();

      if (!mounted) {
        return;
      }

      setState(() {
        _selectedImage = image;

        _selectedImageBytes =
            bytes;

        _resetAnalysis();
      });
    } catch (_) {
      if (!mounted) {
        return;
      }

      _showMessage(
        '이미지를 불러오지 못했습니다.',
      );
    } finally {
      if (mounted) {
        setState(() {
          _isPicking = false;
        });
      }
    }
  }

  void _resetAnalysis() {
    _finalResult = null;

    _latestImageLogId = null;

    _hasRecordedAiResult =
    false;

    _correctionSubmitted = false;

    _usedServerReanalysis =
    false;

    _progressMessage = null;

    _analysisError = null;
  }

  void _clearImage() {
    if (_isBusy) {
      return;
    }

    setState(() {
      _selectedImage = null;

      _selectedImageBytes = null;

      _resetAnalysis();
    });
  }

  /// AI 분석 자체가 완료되지 못해
  /// 정정 데이터를 저장할 수 없는 경우에는
  /// 일반 품목 검색 화면으로 이동합니다.
  ///
  /// 반대로 모바일 AI 결과가 이미 ImageLog에
  /// 저장된 경우에는 사용자의 직접 선택도
  /// 정정 데이터로 기록합니다.
  Future<void>
  _openSearchFromFailure() async {
    final int? imageLogId =
        _latestImageLogId;

    if (_hasRecordedAiResult &&
        imageLogId != null &&
        imageLogId > 0) {
      await _selectAndSaveCorrection(
        imageLogId:
        imageLogId,
      );

      return;
    }

    if (!mounted) {
      return;
    }

    Navigator.pushNamed(
      context,
      AppRoutes.wasteSearch,
    );
  }

  Future<void>
  _analyzeSelectedImage() async {
    final XFile? image =
        _selectedImage;

    final Uint8List? bytes =
        _selectedImageBytes;

    if (image == null ||
        bytes == null) {
      _showMessage(
        '먼저 분석할 사진을 '
            '촬영하거나 선택해주세요.',
      );

      return;
    }

    if (kIsWeb) {
      _showMessage(
        '현재 기기 내 AI 분석은 '
            'Android에서 사용할 수 있어요.',
      );

      return;
    }

    if (_isBusy) {
      return;
    }

    setState(() {
      _isAnalyzing = true;

      _finalResult = null;

      _latestImageLogId = null;

      _hasRecordedAiResult =
      false;

      _analysisError = null;

      _usedServerReanalysis =
      false;

      _progressMessage =
      '휴대폰에서 사진을 분석하고 있어요.';
    });

    try {
      /*
       * 1. TFLite 기기 내 1차 분석
       */
      final TfliteWasteDetectionResult
      tfliteResult =
      await _tfliteDetector
          .analyze(
        imageBytes: bytes,
      );

      if (!mounted) {
        return;
      }

      setState(() {
        _progressMessage =
        '분석 결과를 안전하게 저장하고 있어요.';
      });

      /*
       * 2. 원본 이미지 Spring Boot 업로드
       */
      final ImageUploadResult
      uploadResult =
      await ImageUploadApi.upload(
        bytes: bytes,
        fileName: image.name,
      );

      /*
       * ImageLog ID는 사용자 정정 기능에서도
       * 필요하므로 별도로 기억합니다.
       */
      _latestImageLogId =
          uploadResult.imageLogId;

      /*
       * 3. TFLite 결과 저장
       */
      final MobileAnalysisResult
      mobileResult =
      await MobileAnalysisApi.record(
        imageLogId:
        uploadResult.imageLogId,
        modelLabel:
        tfliteResult.modelLabel,
        confidence:
        tfliteResult.confidence,
        modelVersion:
        tfliteResult.modelVersion,
      );

      /*
       * 여기까지 왔다면 최소한 모바일 AI 결과가
       * ImageLog에 존재하므로 사용자 정정 API를
       * 사용할 수 있습니다.
       */
      _hasRecordedAiResult =
      true;

      /*
       * 4. 모바일 AI 신뢰도가 충분한 경우
       */
      if (!mobileResult
          .needsServerReanalysis) {
        final int? wasteItemId =
            mobileResult.wasteItemId;

        final String?
        wasteItemName =
            mobileResult
                .wasteItemName;

        if (wasteItemId == null ||
            wasteItemId <= 0 ||
            wasteItemName == null ||
            wasteItemName.isEmpty) {
          throw const _AiFlowException(
            'AI 분석 결과를 품목 정보와 '
                '연결하지 못했어요.',
          );
        }

        if (!mounted) {
          return;
        }

        setState(() {
          _finalResult =
              _FinalAiResult(
                imageLogId:
                uploadResult.imageLogId,
                wasteItemId:
                wasteItemId,
                wasteItemName:
                wasteItemName,
                confidence:
                mobileResult.confidence,
                modelVersion:
                mobileResult
                    .modelVersion ??
                    tfliteResult
                        .modelVersion,
                userCorrected:
                false,
              );

          _progressMessage = null;
        });

        return;
      }

      /*
       * 5. 모바일 신뢰도가 낮으면
       *    서버 YOLO를 자동 호출
       */
      if (!mounted) {
        return;
      }

      setState(() {
        _usedServerReanalysis =
        true;

        _progressMessage =
        '조금 더 정확하게 확인하고 있어요.';
      });

      final ServerReanalysisResult
      serverResult =
      await ServerReanalysisApi
          .analyze(
        imageLogId:
        uploadResult.imageLogId,
      );

      /*
       * 서버에서도 충분히 확실한 품목을
       * 찾지 못했다면 억지로 AI 결과를
       * 보여주지 않습니다.
       *
       * 이 경우에도 모바일 AI 결과는
       * ImageLog에 남아 있으므로 사용자가
       * 직접 선택하면 정정 자료로 저장됩니다.
       */
      if (!serverResult.detected) {
        throw const _AiFlowException(
          '현재 AI가 지원하지 않는 품목이거나 '
              '사진만으로 구분하기 어려운 '
              '물건일 수 있어요.',
        );
      }

      final int? wasteItemId =
          serverResult.wasteItemId;

      final String? wasteItemName =
          serverResult
              .wasteItemName;

      if (wasteItemId == null ||
          wasteItemId <= 0 ||
          wasteItemName == null ||
          wasteItemName.isEmpty) {
        throw const _AiFlowException(
          'AI 분석 결과를 품목 정보와 '
              '연결하지 못했어요.',
        );
      }

      if (!mounted) {
        return;
      }

      setState(() {
        _finalResult =
            _FinalAiResult(
              imageLogId:
              uploadResult.imageLogId,
              wasteItemId:
              wasteItemId,
              wasteItemName:
              wasteItemName,
              confidence:
              serverResult.confidence,
              modelVersion:
              serverResult
                  .modelVersion,
              userCorrected:
              false,
            );

        _progressMessage = null;
      });
    } on TfliteWasteDetectorException catch (
    exception
    ) {
      _setAnalysisError(
        exception.message,
      );
    } on ImageUploadApiException catch (
    exception
    ) {
      if (exception.unauthorized) {
        await _moveToLogin();

        return;
      }

      _setAnalysisError(
        exception.message,
      );
    } on MobileAnalysisApiException catch (
    exception
    ) {
      if (exception.unauthorized) {
        await _moveToLogin();

        return;
      }

      _setAnalysisError(
        exception.message,
      );
    } on ServerReanalysisApiException catch (
    exception
    ) {
      if (exception.unauthorized) {
        await _moveToLogin();

        return;
      }

      _setAnalysisError(
        exception.message,
      );
    } on _AiFlowException catch (
    exception
    ) {
      _setAnalysisError(
        exception.message,
      );
    } catch (_) {
      _setAnalysisError(
        'AI 분석 중 문제가 발생했어요. '
            '잠시 후 다시 시도해주세요.',
      );
    } finally {
      if (mounted) {
        setState(() {
          _isAnalyzing = false;

          _progressMessage = null;
        });
      }
    }
  }

  void _setAnalysisError(
      String message,
      ) {
    if (!mounted) {
      return;
    }

    setState(() {
      _finalResult = null;

      _analysisError =
          message;

      _progressMessage = null;
    });
  }

  /// AI 결과가 실제 물건과 맞다고
  /// 사용자가 확인한 경우입니다.
  void _acceptCurrentResult() {
    final _FinalAiResult?
    result =
        _finalResult;

    if (result == null) {
      return;
    }

    _openDisposalCheck(
      result,
    );
  }

  /// AI 결과가 틀린 경우
  /// 기존 품목 검색 화면을 선택 모드로 열고
  /// 사용자의 선택을 ImageLog에 저장합니다.
  Future<void>
  _correctCurrentResult() async {
    final _FinalAiResult?
    result =
        _finalResult;

    if (result == null) {
      return;
    }

    await _selectAndSaveCorrection(
      imageLogId:
      result.imageLogId,
      currentWasteItemId:
      result.wasteItemId,
    );
  }

  /// AI 정정용 품목 검색
  ///
  /// 기존 WasteSearchPage를 그대로 재사용하되
  /// selectionMode=true로 열어 선택된
  /// WasteSearchItem을 이전 화면으로 반환합니다.
  Future<void>
  _selectAndSaveCorrection({
    required int imageLogId,
    int? currentWasteItemId,
  }) async {
    if (_isBusy) {
      return;
    }

    final WasteSearchItem? selected =
    await Navigator.push<
        WasteSearchItem>(
      context,
      MaterialPageRoute<
          WasteSearchItem>(
        builder: (_) =>
        const WasteSearchPage(
          selectionMode: true,
        ),
      ),
    );

    if (!mounted ||
        selected == null) {
      return;
    }

    /*
     * AI가 제시한 품목과 동일한 품목을
     * 다시 선택했다면 정정 요청을 만들
     * 필요가 없습니다.
     */
    if (currentWasteItemId !=
        null &&
        selected.id ==
            currentWasteItemId) {
      _showMessage(
        'AI 추정과 같은 품목을 선택했어요.',
      );

      return;
    }

    setState(() {
      _finalResult = _FinalAiResult(
        imageLogId: imageLogId,
        wasteItemId: selected.id,
        wasteItemName: selected.name,
        confidence: null,
        modelVersion: null,
        userCorrected: true,
      );
      _correctionSubmitted = false;
      _analysisError = null;
    });

    _showMessage(
      '${selected.name}(으)로 품목을 변경했어요.',
    );
  }

  Future<void> _submitCurrentCorrection() async {
    final _FinalAiResult? result = _finalResult;

    if (result == null || !result.userCorrected || _isBusy) {
      return;
    }

    final String? description =
    await _askCorrectionDescription(
      result.wasteItemName,
    );

    if (!mounted || description == null) {
      return;
    }

    setState(() {
      _isCorrecting = true;
    });

    try {
      final ImageCorrectionResult
      correction =
      await ImageCorrectionApi
          .correct(
        imageLogId:
        result.imageLogId,
        wasteItemId:
        result.wasteItemId,
        description:
        description,
      );

      if (!mounted) {
        return;
      }

      setState(() {
        _finalResult =
            _FinalAiResult(
              imageLogId:
              correction.imageLogId,
              wasteItemId:
              correction
                  .correctedWasteItemId,
              wasteItemName:
              correction
                  .correctedWasteItemName,
              confidence:
              null,
              modelVersion:
              null,
              userCorrected:
              true,
            );
        _correctionSubmitted = true;
        _analysisError = null;
      });

      _showMessage(
        '${correction.correctedWasteItemName}(으)로 '
            '관리자에게 정정 요청을 보냈어요.',
      );
    } on ImageCorrectionApiException catch (
    exception
    ) {
      if (!mounted) {
        return;
      }

      if (exception.unauthorized) {
        await _moveToLogin();

        return;
      }

      _showMessage(
        exception.message,
      );
    } catch (_) {
      if (!mounted) {
        return;
      }

      _showMessage(
        '선택한 품목을 저장하지 못했어요.',
      );
    } finally {
      if (mounted) {
        setState(() {
          _isCorrecting = false;
        });
      }
    }
  }

  Future<String?> _askCorrectionDescription(
      String wasteItemName,
      ) async {
    return showDialog<String>(
      context: context,
      builder: (_) => _CorrectionReportDialog(
        wasteItemName: wasteItemName,
      ),
    );
  }

  void _openDisposalCheck(
      _FinalAiResult result,
      ) {
    Navigator.pushNamed(
      context,
      AppRoutes.aiDisposalCheck,
      arguments:
      <String, dynamic>{
        'wasteItemId':
        result.wasteItemId,
        'wasteItemName':
        result.wasteItemName,
        'confidence':
        result.confidence,
        'modelVersion':
        result.modelVersion,
        'userCorrected':
        result.userCorrected,
      },
    );
  }

  Future<void> _moveToLogin() async {
    await TokenStorage.clearTokens();

    if (!mounted) {
      return;
    }

    Navigator.pushNamedAndRemoveUntil(
      context,
      AppRoutes.login,
          (route) => false,
    );
  }

  void _showMessage(
      String message,
      ) {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(
        SnackBar(
          content: Text(
            message,
          ),
        ),
      );
  }

  String _fileSizeText() {
    final Uint8List? bytes =
        _selectedImageBytes;

    if (bytes == null) {
      return '';
    }

    final double megabytes =
        bytes.lengthInBytes /
            (1024 * 1024);

    if (megabytes >= 1) {
      return '${megabytes.toStringAsFixed(1)} MB';
    }

    return '${(bytes.lengthInBytes / 1024).toStringAsFixed(0)} KB';
  }

  @override
  Widget build(
      BuildContext context,
      ) {
    final bool hasImage =
        _selectedImage != null &&
            _selectedImageBytes != null;

    return Scaffold(
      appBar: AppBar(
        title: const Text(
          'AI 품목 분석',
        ),
      ),
      body: SafeArea(
        top: false,
        child: ListView(
          padding:
          const EdgeInsets.fromLTRB(
            20,
            12,
            20,
            40,
          ),
          children: [
            Text(
              '버릴 물건을 촬영하거나 '
                  '사진을 선택해주세요.',
              style: Theme.of(context)
                  .textTheme
                  .titleLarge,
            ),

            const SizedBox(
              height: 8,
            ),

            Text(
              'AI가 사진 속 물건을 먼저 추정하고, '
                  '필요한 경우 더 정확한 분석을 '
                  '자동으로 진행해요.',
              style: Theme.of(context)
                  .textTheme
                  .bodyMedium
                  ?.copyWith(
                height: 1.5,
              ),
            ),

            const SizedBox(
              height: 22,
            ),

            _ImagePreviewCard(
              imageBytes:
              _selectedImageBytes,
              isPicking:
              _isPicking,
            ),

            if (hasImage) ...[
              const SizedBox(
                height: 12,
              ),

              Row(
                children: [
                  Expanded(
                    child: Text(
                      _selectedImage!
                          .name,
                      overflow:
                      TextOverflow
                          .ellipsis,
                      style:
                      const TextStyle(
                        fontSize: 12,
                        color: AppTheme
                            .textSecondaryColor,
                      ),
                    ),
                  ),

                  const SizedBox(
                    width: 10,
                  ),

                  Text(
                    _fileSizeText(),
                    style:
                    const TextStyle(
                      fontSize: 12,
                      color: AppTheme
                          .textSecondaryColor,
                    ),
                  ),
                ],
              ),
            ],

            const SizedBox(
              height: 20,
            ),

            Row(
              children: [
                Expanded(
                  child:
                  OutlinedButton.icon(
                    onPressed:
                    _isBusy
                        ? null
                        : _takePhoto,
                    icon: const Icon(
                      Icons
                          .camera_alt_outlined,
                    ),
                    label: const Text(
                      '촬영하기',
                    ),
                  ),
                ),

                const SizedBox(
                  width: 12,
                ),

                Expanded(
                  child:
                  ElevatedButton.icon(
                    onPressed:
                    _isBusy
                        ? null
                        : _pickFromGallery,
                    icon: const Icon(
                      Icons
                          .photo_library_outlined,
                    ),
                    label: const Text(
                      '사진 선택',
                    ),
                  ),
                ),
              ],
            ),

            if (hasImage &&
                !_isAnalyzing) ...[
              const SizedBox(
                height: 8,
              ),

              TextButton.icon(
                onPressed:
                _isBusy
                    ? null
                    : _clearImage,
                icon: const Icon(
                  Icons.refresh_rounded,
                ),
                label: const Text(
                  '다른 사진 선택하기',
                ),
              ),
            ],

            if (hasImage &&
                _finalResult == null) ...[
              const SizedBox(
                height: 16,
              ),

              SizedBox(
                height: 54,
                child:
                ElevatedButton.icon(
                  onPressed:
                  _isBusy
                      ? null
                      : _analyzeSelectedImage,
                  icon:
                  _isAnalyzing
                      ? const SizedBox(
                    width: 20,
                    height: 20,
                    child:
                    CircularProgressIndicator(
                      strokeWidth:
                      2,
                    ),
                  )
                      : const Icon(
                    Icons
                        .auto_awesome_rounded,
                  ),
                  label: Text(
                    _isAnalyzing
                        ? 'AI 분석 중...'
                        : _analysisError != null
                        ? '같은 사진 다시 분석하기'
                        : 'AI로 분석하기',
                  ),
                ),
              ),
            ],

            if (_isAnalyzing) ...[
              const SizedBox(
                height: 14,
              ),

              _AnalysisProgressCard(
                message:
                _progressMessage ??
                    'AI가 사진을 분석하고 있어요.',
              ),
            ],

            if (_analysisError != null &&
                !_isAnalyzing) ...[
              const SizedBox(
                height: 14,
              ),

              _AnalysisErrorCard(
                message:
                _analysisError!,
                isCorrecting:
                _isCorrecting,
                onSearch:
                _openSearchFromFailure,
                onRetake:
                _takePhoto,
              ),
            ],

            if (_finalResult != null) ...[
              const SizedBox(
                height: 20,
              ),

              AiAnalysisResultCard(
                wasteItemName:
                _finalResult!
                    .wasteItemName,
                confidence:
                _finalResult!
                    .confidence,
                usedServerReanalysis:
                _usedServerReanalysis,
                userCorrected:
                _finalResult!
                    .userCorrected,
                isCorrecting:
                _isCorrecting,
                correctionSubmitted:
                _correctionSubmitted,
                onAccept:
                _acceptCurrentResult,
                onCorrect:
                _correctCurrentResult,
                onReport:
                _submitCurrentCorrection,
              ),
            ],

            const SizedBox(
              height: 26,
            ),

            const _SimpleFlowGuide(),
          ],
        ),
      ),
    );
  }
}

class _ImagePreviewCard
    extends StatelessWidget {
  const _ImagePreviewCard({
    required this.imageBytes,
    required this.isPicking,
  });

  final Uint8List? imageBytes;

  final bool isPicking;

  @override
  Widget build(
      BuildContext context,
      ) {
    return AspectRatio(
      aspectRatio: 1,
      child: Container(
        clipBehavior:
        Clip.antiAlias,
        decoration: BoxDecoration(
          color: const Color(
            0xFFF0F5F2,
          ),
          borderRadius:
          BorderRadius.circular(
            24,
          ),
          border: Border.all(
            color: const Color(
              0xFFDDE7E1,
            ),
          ),
        ),
        child: _buildContent(
          context,
        ),
      ),
    );
  }

  Widget _buildContent(
      BuildContext context,
      ) {
    if (isPicking) {
      return const Center(
        child:
        CircularProgressIndicator(),
      );
    }

    if (imageBytes != null) {
      return Image.memory(
        imageBytes!,
        fit: BoxFit.cover,
      );
    }

    return Padding(
      padding:
      const EdgeInsets.all(
        32,
      ),
      child: Column(
        mainAxisAlignment:
        MainAxisAlignment.center,
        children: [
          Container(
            width: 82,
            height: 82,
            decoration:
            BoxDecoration(
              color: AppTheme
                  .primaryColor
                  .withValues(
                alpha: 0.1,
              ),
              shape:
              BoxShape.circle,
            ),
            child: const Icon(
              Icons
                  .photo_camera_outlined,
              size: 38,
              color: AppTheme
                  .primaryColor,
            ),
          ),

          const SizedBox(
            height: 20,
          ),

          Text(
            '분석할 사진이 아직 없어요',
            style: Theme.of(context)
                .textTheme
                .titleMedium,
          ),

          const SizedBox(
            height: 7,
          ),

          const Text(
            '폐기물이 화면에 잘 보이도록 '
                '촬영해주세요.',
            textAlign:
            TextAlign.center,
          ),
        ],
      ),
    );
  }
}

class _AnalysisProgressCard
    extends StatelessWidget {
  const _AnalysisProgressCard({
    required this.message,
  });

  final String message;

  @override
  Widget build(
      BuildContext context,
      ) {
    return Container(
      padding:
      const EdgeInsets.all(
        18,
      ),
      decoration: BoxDecoration(
        color: AppTheme.primaryColor
            .withValues(
          alpha: 0.07,
        ),
        borderRadius:
        BorderRadius.circular(
          18,
        ),
      ),
      child: Row(
        children: [
          const SizedBox(
            width: 23,
            height: 23,
            child:
            CircularProgressIndicator(
              strokeWidth: 2.5,
            ),
          ),

          const SizedBox(
            width: 14,
          ),

          Expanded(
            child: Column(
              crossAxisAlignment:
              CrossAxisAlignment
                  .start,
              children: [
                const Text(
                  'AI가 확인하고 있어요',
                  style: TextStyle(
                    fontWeight:
                    FontWeight.w800,
                  ),
                ),

                const SizedBox(
                  height: 4,
                ),

                Text(
                  message,
                  style:
                  const TextStyle(
                    color: AppTheme
                        .textSecondaryColor,
                    fontSize: 12,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _AnalysisErrorCard
    extends StatelessWidget {
  const _AnalysisErrorCard({
    required this.message,
    required this.isCorrecting,
    required this.onSearch,
    required this.onRetake,
  });

  final String message;

  final bool isCorrecting;

  final VoidCallback onSearch;

  final VoidCallback onRetake;

  @override
  Widget build(
      BuildContext context,
      ) {
    return Container(
      padding:
      const EdgeInsets.all(
        18,
      ),
      decoration: BoxDecoration(
        color: const Color(
          0xFFFFF9EF,
        ),
        borderRadius:
        BorderRadius.circular(
          20,
        ),
        border: Border.all(
          color: const Color(
            0xFFF0DEC0,
          ),
        ),
      ),
      child: Column(
        crossAxisAlignment:
        CrossAxisAlignment.start,
        children: [
          Row(
            crossAxisAlignment:
            CrossAxisAlignment.start,
            children: [
              Container(
                width: 42,
                height: 42,
                alignment:
                Alignment.center,
                decoration:
                BoxDecoration(
                  color: Colors.white,
                  borderRadius:
                  BorderRadius.circular(
                    13,
                  ),
                ),
                child: const Icon(
                  Icons.search_rounded,
                  color: Color(
                    0xFF8B641F,
                  ),
                ),
              ),

              const SizedBox(
                width: 12,
              ),

              const Expanded(
                child: Column(
                  crossAxisAlignment:
                  CrossAxisAlignment
                      .start,
                  children: [
                    Text(
                      'AI가 품목을 정확히 '
                          '구분하지 못했어요',
                      style:
                      TextStyle(
                        fontWeight:
                        FontWeight.w800,
                        fontSize: 15,
                      ),
                    ),

                    SizedBox(
                      height: 4,
                    ),

                    Text(
                      '촬영을 잘못한 것은 아닐 수 있어요.',
                      style:
                      TextStyle(
                        color: AppTheme
                            .textSecondaryColor,
                        fontSize: 12,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),

          const SizedBox(
            height: 16,
          ),

          Text(
            message,
            style:
            const TextStyle(
              fontSize: 13,
              height: 1.55,
            ),
          ),

          const SizedBox(
            height: 9,
          ),

          const Text(
            '품목을 직접 검색하면 '
                '선택한 품목을 기준으로 '
                '분리배출 방법과 거주지별 '
                '배출 일정을 확인할 수 있어요.',
            style:
            TextStyle(
              fontSize: 13,
              height: 1.55,
            ),
          ),

          const SizedBox(
            height: 14,
          ),

          Container(
            width: double.infinity,
            padding:
            const EdgeInsets.all(
              13,
            ),
            decoration:
            BoxDecoration(
              color: Colors.white
                  .withValues(
                alpha: 0.75,
              ),
              borderRadius:
              BorderRadius.circular(
                13,
              ),
            ),
            child:
            const Text(
              '현재 AI 우선 인식 품목\n'
                  '종이박스 · 페트병 · 플라스틱 용기 · '
                  '캔 · 유리병 · 스티로폼',
              style:
              TextStyle(
                color: AppTheme
                    .textSecondaryColor,
                fontSize: 11,
                height: 1.55,
              ),
            ),
          ),

          const SizedBox(
            height: 18,
          ),

          SizedBox(
            width: double.infinity,
            height: 50,
            child:
            ElevatedButton.icon(
              onPressed:
              isCorrecting
                  ? null
                  : onSearch,
              icon:
              isCorrecting
                  ? const SizedBox(
                width: 18,
                height: 18,
                child:
                CircularProgressIndicator(
                  strokeWidth:
                  2,
                ),
              )
                  : const Icon(
                Icons
                    .search_rounded,
              ),
              label: Text(
                isCorrecting
                    ? '선택한 품목 저장 중...'
                    : '품목 직접 검색하기',
              ),
            ),
          ),

          const SizedBox(
            height: 9,
          ),

          SizedBox(
            width: double.infinity,
            height: 48,
            child:
            OutlinedButton.icon(
              onPressed:
              isCorrecting
                  ? null
                  : onRetake,
              icon:
              const Icon(
                Icons
                    .photo_camera_outlined,
              ),
              label:
              const Text(
                '다시 촬영하기',
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _SimpleFlowGuide
    extends StatelessWidget {
  const _SimpleFlowGuide();

  @override
  Widget build(
      BuildContext context,
      ) {
    return Container(
      padding:
      const EdgeInsets.all(
        18,
      ),
      decoration: BoxDecoration(
        color: const Color(
          0xFFF7F9F8,
        ),
        borderRadius:
        BorderRadius.circular(
          18,
        ),
      ),
      child: const Column(
        crossAxisAlignment:
        CrossAxisAlignment.start,
        children: [
          Text(
            '더 정확하게 분석하려면',
            style:
            TextStyle(
              fontSize: 15,
              fontWeight:
              FontWeight.w800,
            ),
          ),

          SizedBox(
            height: 14,
          ),

          _GuideRow(
            icon:
            Icons
                .center_focus_strong,
            text:
            '버릴 물건 하나가 잘 보이도록 촬영해주세요.',
          ),

          SizedBox(
            height: 12,
          ),

          _GuideRow(
            icon:
            Icons
                .wb_sunny_outlined,
            text:
            '너무 어둡거나 역광인 장소는 피해주세요.',
          ),

          SizedBox(
            height: 12,
          ),

          _GuideRow(
            icon:
            Icons.layers_outlined,
            text:
            '여러 물건이 겹치지 않도록 분리해서 촬영해주세요.',
          ),
        ],
      ),
    );
  }
}

class _GuideRow
    extends StatelessWidget {
  const _GuideRow({
    required this.icon,
    required this.text,
  });

  final IconData icon;

  final String text;

  @override
  Widget build(
      BuildContext context,
      ) {
    return Row(
      crossAxisAlignment:
      CrossAxisAlignment.start,
      children: [
        Icon(
          icon,
          size: 19,
          color:
          AppTheme.primaryColor,
        ),

        const SizedBox(
          width: 10,
        ),

        Expanded(
          child: Text(
            text,
            style:
            const TextStyle(
              fontSize: 13,
              height: 1.45,
            ),
          ),
        ),
      ],
    );
  }
}

class _CorrectionReportDialog extends StatefulWidget {
  const _CorrectionReportDialog({
    required this.wasteItemName,
  });

  final String wasteItemName;

  @override
  State<_CorrectionReportDialog> createState() =>
      _CorrectionReportDialogState();
}

class _CorrectionReportDialogState
    extends State<_CorrectionReportDialog> {
  final GlobalKey<FormState> _formKey = GlobalKey<FormState>();
  final TextEditingController _controller = TextEditingController();

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  void _submit() {
    if (!(_formKey.currentState?.validate() ?? false)) {
      return;
    }

    Navigator.pop(
      context,
      _controller.text.trim(),
    );
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: const Text('관리자에게 정정 보내기'),
      content: Form(
        key: _formKey,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              '선택 품목: ${widget.wasteItemName}',
              style: const TextStyle(
                fontWeight: FontWeight.w700,
              ),
            ),
            const SizedBox(height: 10),
            const Text(
              '인식에 사용한 사진과 선택한 품목이 함께 전달돼요. '
                  '관리자가 확인할 수 있도록 물품 특징을 적어주세요.',
            ),
            const SizedBox(height: 16),
            TextFormField(
              controller: _controller,
              autofocus: true,
              minLines: 3,
              maxLines: 5,
              maxLength: 500,
              textInputAction: TextInputAction.done,
              onFieldSubmitted: (_) => _submit(),
              validator: (value) {
                if (value == null || value.trim().isEmpty) {
                  return '관리자가 확인할 수 있도록 설명을 입력해주세요.';
                }
                return null;
              },
              decoration: const InputDecoration(
                labelText: '물품 설명',
                hintText: '예: 투명한 일회용 플라스틱 컵이에요.',
                alignLabelWithHint: true,
              ),
            ),
          ],
        ),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(context),
          child: const Text('취소'),
        ),
        FilledButton.icon(
          onPressed: _submit,
          icon: const Icon(Icons.send_rounded),
          label: const Text('관리자에게 보내기'),
        ),
      ],
    );
  }
}

class _FinalAiResult {
  const _FinalAiResult({
    required this.imageLogId,
    required this.wasteItemId,
    required this.wasteItemName,
    required this.confidence,
    required this.modelVersion,
    required this.userCorrected,
  });

  final int imageLogId;

  final int wasteItemId;

  final String wasteItemName;

  final double? confidence;

  final String? modelVersion;

  final bool userCorrected;
}

class _AiFlowException
    implements Exception {
  const _AiFlowException(
      this.message,
      );

  final String message;
}
