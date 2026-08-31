import 'dart:typed_data';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import 'package:smart_recycle/app/app_routes.dart';
import 'package:smart_recycle/core/storage/token_storage.dart';
import 'package:smart_recycle/core/theme/app_theme.dart';
import 'package:smart_recycle/features/ai/data/ai_mock_analysis_api.dart';
import 'package:smart_recycle/features/ai/data/image_upload_api.dart';
import 'package:smart_recycle/features/ai/data/server_reanalysis_api.dart';
import 'package:smart_recycle/features/waste/data/waste_search_api.dart';

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

  XFile? _selectedImage;
  Uint8List? _selectedImageBytes;

  ImageUploadResult? _uploadResult;

  List<WasteSearchItem>
  _mockWasteItems = [];

  int? _selectedMockWasteItemId;

  MockAnalysisScenario _mockScenario =
      MockAnalysisScenario.lowConfidence;

  MockAnalysisResult? _mockResult;

  ServerReanalysisResult?
  _serverResult;

  bool _isPicking = false;
  bool _isUploading = false;

  bool _isLoadingMockItems = false;
  bool _isMockAnalyzing = false;

  bool _isServerAnalyzing = false;

  String? _mockItemsError;

  Future<void> _pickFromGallery() async {
    await _pickImage(
      source: ImageSource.gallery,
    );
  }

  Future<void> _takePhoto() async {
    if (kIsWeb) {
      _showMessage(
        '현재 Chrome 테스트에서는 사진 선택을 사용해주세요. '
            '실제 Android 기기에서는 카메라 촬영을 사용할 수 있어요.',
      );

      return;
    }

    await _pickImage(
      source: ImageSource.camera,
    );
  }

  Future<void> _pickImage({
    required ImageSource source,
  }) async {
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
        _selectedImageBytes = bytes;

        _uploadResult = null;

        _mockResult = null;
        _serverResult = null;

        _mockWasteItems = [];
        _selectedMockWasteItemId =
        null;

        _mockItemsError = null;

        _mockScenario =
            MockAnalysisScenario
                .lowConfidence;
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

  bool get _isBusy {
    return _isPicking ||
        _isUploading ||
        _isMockAnalyzing ||
        _isServerAnalyzing;
  }

  void _clearImage() {
    if (_isBusy) {
      return;
    }

    setState(() {
      _selectedImage = null;
      _selectedImageBytes = null;

      _uploadResult = null;

      _mockResult = null;
      _serverResult = null;

      _mockWasteItems = [];
      _selectedMockWasteItemId =
      null;

      _mockItemsError = null;

      _mockScenario =
          MockAnalysisScenario
              .lowConfidence;
    });
  }

  Future<void> _uploadImage() async {
    final XFile? image =
        _selectedImage;

    final Uint8List? bytes =
        _selectedImageBytes;

    if (image == null ||
        bytes == null) {
      _showMessage(
        '먼저 분석할 사진을 선택해주세요.',
      );

      return;
    }

    if (_uploadResult != null) {
      _showMessage(
        '이미 서버에 업로드된 이미지예요.',
      );

      return;
    }

    if (_isBusy) {
      return;
    }

    setState(() {
      _isUploading = true;
    });

    try {
      final ImageUploadResult result =
      await ImageUploadApi.upload(
        bytes: bytes,
        fileName: image.name,
      );

      if (!mounted) {
        return;
      }

      setState(() {
        _uploadResult = result;
      });

      _showMessage(
        '이미지를 서버에 업로드했어요.',
      );

      if (kDebugMode) {
        await _loadMockWasteItems();
      }
    } on ImageUploadApiException catch (
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
        '이미지를 업로드하는 중 '
            '오류가 발생했습니다.',
      );
    } finally {
      if (mounted) {
        setState(() {
          _isUploading = false;
        });
      }
    }
  }

  Future<void>
  _loadMockWasteItems() async {
    if (_isLoadingMockItems) {
      return;
    }

    setState(() {
      _isLoadingMockItems = true;
      _mockItemsError = null;
    });

    try {
      final WasteSearchResult result =
      await WasteSearchApi
          .searchItems(
        keyword: '',
        page: 0,
        size: 100,
      );

      if (!mounted) {
        return;
      }

      setState(() {
        _mockWasteItems =
            result.items;

        if (_mockWasteItems.isNotEmpty) {
          _selectedMockWasteItemId =
              _findDefaultMockItemId(
                _mockWasteItems,
              );
        }
      });
    } on WasteSearchApiException catch (
    exception
    ) {
      if (!mounted) {
        return;
      }

      if (exception.unauthorized) {
        await _moveToLogin();
        return;
      }

      setState(() {
        _mockItemsError =
            exception.message;
      });
    } catch (_) {
      if (!mounted) {
        return;
      }

      setState(() {
        _mockItemsError =
        '품목 목록을 불러오지 못했습니다.';
      });
    } finally {
      if (mounted) {
        setState(() {
          _isLoadingMockItems = false;
        });
      }
    }
  }

  int _findDefaultMockItemId(
      List<WasteSearchItem> items,
      ) {
    for (
    final WasteSearchItem item
    in items
    ) {
      if (item.name == '플라스틱 용기') {
        return item.id;
      }
    }

    return items.first.id;
  }

  Future<void>
  _runMockAnalysis() async {
    final ImageUploadResult? upload =
        _uploadResult;

    if (upload == null) {
      _showMessage(
        '먼저 이미지를 업로드해주세요.',
      );

      return;
    }

    if (_mockResult != null) {
      _showMessage(
        '현재 이미지는 이미 1차 분석됐어요.',
      );

      return;
    }

    if (_mockScenario
        .requiresWasteItem &&
        _selectedMockWasteItemId ==
            null) {
      _showMessage(
        '테스트할 품목을 선택해주세요.',
      );

      return;
    }

    if (_isBusy) {
      return;
    }

    setState(() {
      _isMockAnalyzing = true;
    });

    try {
      final MockAnalysisResult result =
      await AiMockAnalysisApi
          .analyze(
        imageLogId:
        upload.imageLogId,
        scenario:
        _mockScenario,
        wasteItemId:
        _selectedMockWasteItemId,
      );

      if (!mounted) {
        return;
      }

      setState(() {
        _mockResult = result;
      });

      if (result
          .needsServerReanalysis) {
        _showMessage(
          '낮은 신뢰도로 판정됐어요. '
              '이제 실제 Python YOLO로 재분석할 수 있어요.',
        );
      } else if (
      result.analysisStatus ==
          'ANALYSIS_FAILED'
      ) {
        _showMessage(
          '분석 실패 시나리오가 적용됐어요.',
        );
      } else {
        _showMessage(
          '높은 신뢰도 분석이 완료됐어요.',
        );
      }
    } on AiMockAnalysisApiException catch (
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
        'AI 분석 중 오류가 발생했습니다.',
      );
    } finally {
      if (mounted) {
        setState(() {
          _isMockAnalyzing = false;
        });
      }
    }
  }

  Future<void>
  _runServerReanalysis() async {
    final ImageUploadResult? upload =
        _uploadResult;

    final MockAnalysisResult? mock =
        _mockResult;

    if (upload == null ||
        mock == null) {
      _showMessage(
        '먼저 낮은 신뢰도 1차 분석을 실행해주세요.',
      );

      return;
    }

    if (!mock.needsServerReanalysis) {
      _showMessage(
        '현재 결과는 서버 재분석 대상이 아닙니다.',
      );

      return;
    }

    if (_serverResult != null) {
      _showMessage(
        '이미 서버 AI 재분석을 완료했습니다.',
      );

      return;
    }

    if (_isBusy) {
      return;
    }

    setState(() {
      _isServerAnalyzing = true;
    });

    try {
      final ServerReanalysisResult result =
      await ServerReanalysisApi
          .analyze(
        imageLogId:
        upload.imageLogId,
      );

      if (!mounted) {
        return;
      }

      setState(() {
        _serverResult = result;
      });

      if (result.detected) {
        _showMessage(
          'Python YOLO 재분석이 완료됐어요.',
        );
      } else {
        _showMessage(
          'YOLO가 사진에서 지원 품목을 '
              '찾지 못했어요.',
        );
      }
    } on ServerReanalysisApiException catch (
    exception
    ) {
      if (!mounted) {
        return;
      }

      if (exception.unauthorized) {
        await _moveToLogin();
        return;
      }

      /*
       * AI 서버 연결 실패/Timeout 등의 경우
       * 백엔드는 SERVER_REANALYSIS_PENDING을
       * 유지하므로 사용자가 다시 시도할 수 있습니다.
       */
      _showMessage(
        exception.message,
      );
    } catch (_) {
      if (!mounted) {
        return;
      }

      _showMessage(
        '서버 AI 재분석 중 오류가 발생했습니다.',
      );
    } finally {
      if (mounted) {
        setState(() {
          _isServerAnalyzing = false;
        });
      }
    }
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

  void _openDisposalCheck() {
    final ServerReanalysisResult?
    result =
        _serverResult;

    if (result == null ||
        result.wasteItemId == null ||
        result.wasteItemName == null) {
      _showMessage(
        '연결된 분리배출 품목이 없습니다.',
      );

      return;
    }

    Navigator.pushNamed(
      context,
      AppRoutes.aiDisposalCheck,
      arguments: <String, dynamic>{
        'wasteItemId':
        result.wasteItemId,
        'wasteItemName':
        result.wasteItemName,
        'confidence':
        result.confidence,
        'modelVersion':
        result.modelVersion,
      },
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

    final double kilobytes =
        bytes.lengthInBytes / 1024;

    return '${kilobytes.toStringAsFixed(0)} KB';
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
            36,
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
              '사진 속 폐기물을 AI로 확인한 뒤 '
                  '분리배출 방법과 내 거주지 일정을 '
                  '함께 안내해요.',
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
              isPicking: _isPicking,
            ),

            if (hasImage) ...[
              const SizedBox(
                height: 12,
              ),

              Row(
                children: [
                  Expanded(
                    child: Text(
                      _selectedImage!.name,
                      overflow:
                      TextOverflow.ellipsis,
                    ),
                  ),

                  const SizedBox(
                    width: 12,
                  ),

                  Text(
                    _fileSizeText(),
                    style:
                    const TextStyle(
                      color: AppTheme
                          .textSecondaryColor,
                      fontSize: 12,
                    ),
                  ),
                ],
              ),
            ],

            if (_uploadResult != null) ...[
              const SizedBox(
                height: 14,
              ),

              _UploadCompletedCard(
                result:
                _uploadResult!,
              ),
            ],

            const SizedBox(
              height: 22,
            ),

            Row(
              children: [
                Expanded(
                  child:
                  OutlinedButton.icon(
                    onPressed: _isBusy
                        ? null
                        : _takePhoto,
                    icon: const Icon(
                      Icons
                          .camera_alt_outlined,
                    ),
                    label: Text(
                      kIsWeb
                          ? '카메라'
                          : '촬영하기',
                    ),
                  ),
                ),

                const SizedBox(
                  width: 12,
                ),

                Expanded(
                  child:
                  ElevatedButton.icon(
                    onPressed: _isBusy
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

            if (hasImage) ...[
              const SizedBox(
                height: 12,
              ),

              TextButton.icon(
                onPressed: _isBusy
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

            const SizedBox(
              height: 20,
            ),

            SizedBox(
              height: 52,
              child:
              ElevatedButton.icon(
                onPressed:
                hasImage &&
                    !_isBusy &&
                    _uploadResult ==
                        null
                    ? _uploadImage
                    : null,
                icon: _isUploading
                    ? const SizedBox(
                  width: 20,
                  height: 20,
                  child:
                  CircularProgressIndicator(
                    strokeWidth: 2,
                  ),
                )
                    : const Icon(
                  Icons
                      .cloud_upload_outlined,
                ),
                label: Text(
                  _isUploading
                      ? '이미지 업로드 중...'
                      : _uploadResult != null
                      ? '이미지 업로드 완료'
                      : '이미지 업로드하기',
                ),
              ),
            ),

            if (kDebugMode &&
                _uploadResult != null) ...[
              const SizedBox(
                height: 22,
              ),

              _buildMockSection(),
            ],

            if (_serverResult != null) ...[
              const SizedBox(
                height: 22,
              ),

              _ServerResultCard(
                result:
                _serverResult!,
                onOpenDetail:
                _serverResult!
                    .wasteItemId !=
                    null
                    ? _openDisposalCheck
                    : null,
              ),
            ],

            const SizedBox(
              height: 22,
            ),

            const _AnalysisFlowCard(),
          ],
        ),
      ),
    );
  }

  Widget _buildMockSection() {
    return Container(
      padding:
      const EdgeInsets.all(
        18,
      ),
      decoration: BoxDecoration(
        color: const Color(
          0xFFFFF8E8,
        ),
        borderRadius:
        BorderRadius.circular(
          18,
        ),
        border: Border.all(
          color: const Color(
            0xFFF0DFC0,
          ),
        ),
      ),
      child: Column(
        crossAxisAlignment:
        CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Icon(
                Icons
                    .developer_mode_rounded,
              ),

              const SizedBox(
                width: 9,
              ),

              Expanded(
                child: Text(
                  'Chrome 개발 테스트',
                  style:
                  Theme.of(context)
                      .textTheme
                      .titleMedium,
                ),
              ),
            ],
          ),

          const SizedBox(
            height: 8,
          ),

          const Text(
            '실제 모바일 TFLite 대신 '
                'Mock 결과를 먼저 만들어 '
                '저신뢰도 서버 재분석 흐름을 테스트합니다.',
          ),

          const SizedBox(
            height: 18,
          ),

          if (_isLoadingMockItems)
            const Center(
              child:
              CircularProgressIndicator(),
            )
          else if (_mockItemsError !=
              null) ...[
            Text(
              _mockItemsError!,
            ),

            const SizedBox(
              height: 10,
            ),

            OutlinedButton(
              onPressed:
              _loadMockWasteItems,
              child: const Text(
                '품목 다시 불러오기',
              ),
            ),
          ] else ...[
            DropdownButtonFormField<int>(
              value:
              _selectedMockWasteItemId,
              decoration:
              const InputDecoration(
                labelText:
                '1차 분석 가정 품목',
              ),
              items: _mockWasteItems
                  .map(
                    (item) =>
                    DropdownMenuItem<int>(
                      value: item.id,
                      child: Text(
                        item.name,
                      ),
                    ),
              )
                  .toList(),
              onChanged:
              _mockResult != null
                  ? null
                  : (value) {
                setState(() {
                  _selectedMockWasteItemId =
                      value;
                });
              },
            ),

            const SizedBox(
              height: 12,
            ),

            DropdownButtonFormField<
                MockAnalysisScenario>(
              value: _mockScenario,
              decoration:
              const InputDecoration(
                labelText:
                '1차 분석 시나리오',
              ),
              items:
              MockAnalysisScenario
                  .values
                  .map(
                    (scenario) {
                  return DropdownMenuItem<
                      MockAnalysisScenario>(
                    value: scenario,
                    child: Text(
                      scenario.label,
                    ),
                  );
                },
              ).toList(),
              onChanged:
              _mockResult != null
                  ? null
                  : (value) {
                if (value ==
                    null) {
                  return;
                }

                setState(() {
                  _mockScenario =
                      value;
                });
              },
            ),

            const SizedBox(
              height: 16,
            ),

            SizedBox(
              width: double.infinity,
              child:
              ElevatedButton.icon(
                onPressed:
                _isBusy ||
                    _mockResult !=
                        null
                    ? null
                    : _runMockAnalysis,
                icon: _isMockAnalyzing
                    ? const SizedBox(
                  width: 18,
                  height: 18,
                  child:
                  CircularProgressIndicator(
                    strokeWidth: 2,
                  ),
                )
                    : const Icon(
                  Icons
                      .science_outlined,
                ),
                label: Text(
                  _isMockAnalyzing
                      ? '1차 분석 중...'
                      : '개발용 1차 분석 실행',
                ),
              ),
            ),
          ],

          if (_mockResult != null) ...[
            const SizedBox(
              height: 16,
            ),

            _MockResultCard(
              result:
              _mockResult!,
            ),

            if (_mockResult!
                .needsServerReanalysis &&
                _serverResult == null) ...[
              const SizedBox(
                height: 16,
              ),

              SizedBox(
                width: double.infinity,
                child:
                ElevatedButton.icon(
                  onPressed:
                  _isBusy
                      ? null
                      : _runServerReanalysis,
                  icon: _isServerAnalyzing
                      ? const SizedBox(
                    width: 18,
                    height: 18,
                    child:
                    CircularProgressIndicator(
                      strokeWidth: 2,
                    ),
                  )
                      : const Icon(
                    Icons
                        .hub_outlined,
                  ),
                  label: Text(
                    _isServerAnalyzing
                        ? 'Python YOLO 분석 중...'
                        : '실제 서버 AI로 재분석',
                  ),
                ),
              ),
            ],
          ],
        ],
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

    final Uint8List? bytes =
        imageBytes;

    if (bytes != null) {
      return Image.memory(
        bytes,
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
              shape: BoxShape.circle,
            ),
            child: const Icon(
              Icons
                  .photo_camera_outlined,
              size: 38,
              color:
              AppTheme.primaryColor,
            ),
          ),

          const SizedBox(
            height: 20,
          ),

          Text(
            '분석할 사진이 아직 없어요',
            style:
            Theme.of(context)
                .textTheme
                .titleMedium,
          ),

          const SizedBox(
            height: 7,
          ),

          Text(
            '폐기물이 화면에 잘 보이도록 '
                '가까이에서 촬영해주세요.',
            textAlign:
            TextAlign.center,
          ),
        ],
      ),
    );
  }
}

class _UploadCompletedCard
    extends StatelessWidget {
  const _UploadCompletedCard({
    required this.result,
  });

  final ImageUploadResult result;

  @override
  Widget build(
      BuildContext context,
      ) {
    return Container(
      padding:
      const EdgeInsets.all(
        16,
      ),
      decoration: BoxDecoration(
        color: AppTheme.primaryColor
            .withValues(
          alpha: 0.08,
        ),
        borderRadius:
        BorderRadius.circular(
          16,
        ),
      ),
      child: Row(
        children: [
          const Icon(
            Icons.cloud_done_rounded,
            color:
            AppTheme.primaryColor,
          ),

          const SizedBox(
            width: 12,
          ),

          Expanded(
            child: Column(
              crossAxisAlignment:
              CrossAxisAlignment.start,
              children: [
                const Text(
                  '이미지 업로드 완료',
                  style: TextStyle(
                    color:
                    AppTheme.primaryColor,
                    fontWeight:
                    FontWeight.w700,
                  ),
                ),

                const SizedBox(
                  height: 3,
                ),

                Text(
                  'ImageLog #'
                      '${result.imageLogId} · '
                      '${result.analysisStatus}',
                  style:
                  const TextStyle(
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

class _MockResultCard
    extends StatelessWidget {
  const _MockResultCard({
    required this.result,
  });

  final MockAnalysisResult result;

  @override
  Widget build(
      BuildContext context,
      ) {
    final double? confidence =
        result.confidence;

    return Container(
      width: double.infinity,
      padding:
      const EdgeInsets.all(
        14,
      ),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius:
        BorderRadius.circular(
          14,
        ),
      ),
      child: Column(
        crossAxisAlignment:
        CrossAxisAlignment.start,
        children: [
          const Text(
            '1차 분석 결과',
            style: TextStyle(
              fontWeight:
              FontWeight.w700,
            ),
          ),

          const SizedBox(
            height: 10,
          ),

          _ResultRow(
            label: '상태',
            value:
            result.analysisStatus,
          ),

          if (result.wasteItemName !=
              null)
            _ResultRow(
              label: '가정 품목',
              value:
              result.wasteItemName!,
            ),

          if (confidence != null)
            _ResultRow(
              label: '신뢰도',
              value:
              '${(confidence * 100).toStringAsFixed(0)}%',
            ),

          _ResultRow(
            label: '서버 재분석',
            value: result
                .needsServerReanalysis
                ? '필요'
                : '불필요',
          ),
        ],
      ),
    );
  }
}

class _ServerResultCard
    extends StatelessWidget {
  const _ServerResultCard({
    required this.result,
    required this.onOpenDetail,
  });

  final ServerReanalysisResult result;

  final VoidCallback? onOpenDetail;

  @override
  Widget build(
      BuildContext context,
      ) {
    final double? confidence =
        result.confidence;

    return Container(
      padding:
      const EdgeInsets.all(
        18,
      ),
      decoration: BoxDecoration(
        color: AppTheme.primaryColor
            .withValues(
          alpha: 0.08,
        ),
        borderRadius:
        BorderRadius.circular(
          20,
        ),
        border: Border.all(
          color: AppTheme.primaryColor
              .withValues(
            alpha: 0.2,
          ),
        ),
      ),
      child: Column(
        crossAxisAlignment:
        CrossAxisAlignment.start,
        children: [
          Row(
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
                  Icons
                      .auto_awesome_rounded,
                  color: AppTheme
                      .primaryColor,
                ),
              ),

              const SizedBox(
                width: 12,
              ),

              Expanded(
                child: Column(
                  crossAxisAlignment:
                  CrossAxisAlignment.start,
                  children: [
                    const Text(
                      'AI 추정 결과',
                      style: TextStyle(
                        fontWeight:
                        FontWeight.w800,
                      ),
                    ),

                    Text(
                      result.detected
                          ? 'Python YOLO가 사진의 '
                          '형태를 바탕으로 품목을 '
                          '추정했어요.'
                          : '지원하는 품목을 '
                          '찾지 못했어요.',
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

          const SizedBox(
            height: 16,
          ),

          _ResultRow(
            label: '상태',
            value:
            result.analysisStatus,
          ),

          if (result.label != null)
            _ResultRow(
              label: 'AI Label',
              value: result.label!,
            ),

          if (result.wasteItemName !=
              null)
            _ResultRow(
              label: '추정 품목',
              value:
              result.wasteItemName!,
            ),

          if (confidence != null)
            _ResultRow(
              label: '신뢰도',
              value:
              '${(confidence * 100).toStringAsFixed(1)}%',
            ),

          _ResultRow(
            label: '탐지 개수',
            value:
            '${result.detectionCount}',
          ),

          if (result.modelVersion !=
              null)
            _ResultRow(
              label: '모델',
              value:
              result.modelVersion!,
            ),

          if (result.detected) ...[
            const SizedBox(
              height: 12,
            ),

            Container(
              width:
              double.infinity,
              padding:
              const EdgeInsets.all(
                13,
              ),
              decoration:
              BoxDecoration(
                color:
                const Color(
                  0xFFFFF7E8,
                ),
                borderRadius:
                BorderRadius.circular(
                  13,
                ),
              ),
              child: const Row(
                crossAxisAlignment:
                CrossAxisAlignment.start,
                children: [
                  Icon(
                    Icons
                        .info_outline_rounded,
                    size: 19,
                  ),

                  SizedBox(
                    width: 8,
                  ),

                  Expanded(
                    child: Text(
                      'AI가 인식한 품목과 '
                          '실제 재활용 가능 여부는 '
                          '다를 수 있습니다. '
                          '재질 표시와 오염 상태를 '
                          '직접 확인해주세요.',
                      style:
                      TextStyle(
                        fontSize: 12,
                        height: 1.4,
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ],

          if (onOpenDetail != null) ...[
            const SizedBox(
              height: 14,
            ),

            SizedBox(
              width: double.infinity,
              child:
              ElevatedButton.icon(
                onPressed:
                onOpenDetail,
                icon: const Icon(
                  Icons
                      .fact_check_outlined,
                ),
                label: const Text(
                  '배출 전 확인하기',
                ),
              ),
            ),
          ],
        ],
      ),
    );
  }
}

class _ResultRow
    extends StatelessWidget {
  const _ResultRow({
    required this.label,
    required this.value,
  });

  final String label;
  final String value;

  @override
  Widget build(
      BuildContext context,
      ) {
    return Padding(
      padding:
      const EdgeInsets.only(
        bottom: 7,
      ),
      child: Row(
        crossAxisAlignment:
        CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 92,
            child: Text(
              label,
              style:
              const TextStyle(
                color: AppTheme
                    .textSecondaryColor,
              ),
            ),
          ),

          Expanded(
            child: Text(
              value,
            ),
          ),
        ],
      ),
    );
  }
}

class _AnalysisFlowCard
    extends StatelessWidget {
  const _AnalysisFlowCard();

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
      child: Column(
        crossAxisAlignment:
        CrossAxisAlignment.start,
        children: [
          Text(
            'AI 분석 흐름',
            style:
            Theme.of(context)
                .textTheme
                .titleMedium,
          ),

          const SizedBox(
            height: 14,
          ),

          const _FlowRow(
            number: '1',
            text:
            'Flutter에서 사진을 선택하거나 촬영',
          ),

          const _FlowRow(
            number: '2',
            text:
            '원본 이미지를 서버에 안전하게 저장',
          ),

          const _FlowRow(
            number: '3',
            text:
            '모바일 AI가 품목을 1차 분석',
          ),

          const _FlowRow(
            number: '4',
            text:
            '신뢰도가 낮으면 Python YOLO가 실제 이미지 재분석',
          ),

          const _FlowRow(
            number: '5',
            text:
            '분리배출 가이드와 내 지역 일정 안내',
            last: true,
          ),
        ],
      ),
    );
  }
}

class _FlowRow
    extends StatelessWidget {
  const _FlowRow({
    required this.number,
    required this.text,
    this.last = false,
  });

  final String number;
  final String text;

  final bool last;

  @override
  Widget build(
      BuildContext context,
      ) {
    return Padding(
      padding: EdgeInsets.only(
        bottom:
        last ? 0 : 12,
      ),
      child: Row(
        children: [
          Container(
            width: 26,
            height: 26,
            alignment:
            Alignment.center,
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
            child: Text(
              number,
              style:
              const TextStyle(
                color: AppTheme
                    .primaryColor,
                fontWeight:
                FontWeight.w700,
                fontSize: 12,
              ),
            ),
          ),

          const SizedBox(
            width: 10,
          ),

          Expanded(
            child: Text(
              text,
            ),
          ),
        ],
      ),
    );
  }
}