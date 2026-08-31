import 'dart:typed_data';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import 'package:smart_recycle/core/theme/app_theme.dart';

class AiCapturePage extends StatefulWidget {
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

  bool _isPicking = false;

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
    if (_isPicking) {
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

  void _clearImage() {
    setState(() {
      _selectedImage = null;
      _selectedImageBytes = null;
    });
  }

  void _startAnalysis() {
    if (_selectedImage == null ||
        _selectedImageBytes == null) {
      _showMessage(
        '먼저 분석할 사진을 선택해주세요.',
      );

      return;
    }

    _showMessage(
      '사진 선택까지 완료됐어요. '
          '다음 단계에서 AI 분석을 연결합니다.',
    );
  }

  void _showMessage(
      String message,
      ) {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(
        SnackBar(
          content: Text(message),
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
  Widget build(BuildContext context) {
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

            const SizedBox(height: 8),

            Text(
              '사진 속 폐기물을 AI로 확인한 뒤 '
                  '분리배출 방법과 내 거주지 일정을 '
                  '함께 안내할 예정이에요.',
              style: Theme.of(context)
                  .textTheme
                  .bodyMedium
                  ?.copyWith(
                height: 1.5,
              ),
            ),

            const SizedBox(height: 22),

            _ImagePreviewCard(
              imageBytes:
              _selectedImageBytes,
              isPicking: _isPicking,
            ),

            if (hasImage) ...[
              const SizedBox(height: 12),

              Row(
                children: [
                  Expanded(
                    child: Text(
                      _selectedImage!.name,
                      overflow:
                      TextOverflow.ellipsis,
                      style: Theme.of(context)
                          .textTheme
                          .bodyMedium,
                    ),
                  ),

                  const SizedBox(width: 12),

                  Text(
                    _fileSizeText(),
                    style: Theme.of(context)
                        .textTheme
                        .bodyMedium
                        ?.copyWith(
                      color: AppTheme
                          .textSecondaryColor,
                    ),
                  ),
                ],
              ),
            ],

            const SizedBox(height: 22),

            Row(
              children: [
                Expanded(
                  child: OutlinedButton.icon(
                    onPressed: _isPicking
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

                const SizedBox(width: 12),

                Expanded(
                  child: ElevatedButton.icon(
                    onPressed: _isPicking
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
              const SizedBox(height: 12),

              TextButton.icon(
                onPressed: _isPicking
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

            const SizedBox(height: 24),

            SizedBox(
              height: 52,
              child: ElevatedButton.icon(
                onPressed:
                hasImage && !_isPicking
                    ? _startAnalysis
                    : null,
                icon: const Icon(
                  Icons.auto_awesome_rounded,
                ),
                label: const Text(
                  'AI로 분석하기',
                ),
              ),
            ),

            const SizedBox(height: 22),

            const _AnalysisFlowCard(),
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
  Widget build(BuildContext context) {
    return AspectRatio(
      aspectRatio: 1,
      child: Container(
        clipBehavior: Clip.antiAlias,
        decoration: BoxDecoration(
          color: const Color(
            0xFFF0F5F2,
          ),
          borderRadius:
          BorderRadius.circular(24),
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
        child: CircularProgressIndicator(),
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
      const EdgeInsets.all(32),
      child: Column(
        mainAxisAlignment:
        MainAxisAlignment.center,
        children: [
          Container(
            width: 82,
            height: 82,
            decoration: BoxDecoration(
              color: AppTheme.primaryColor
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

          const SizedBox(height: 20),

          Text(
            '분석할 사진이 아직 없어요',
            style: Theme.of(context)
                .textTheme
                .titleMedium,
          ),

          const SizedBox(height: 7),

          Text(
            '폐기물이 화면에 잘 보이도록 '
                '가까이에서 촬영해주세요.',
            textAlign:
            TextAlign.center,
            style: Theme.of(context)
                .textTheme
                .bodyMedium,
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
  Widget build(BuildContext context) {
    return Container(
      padding:
      const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: const Color(
          0xFFF7F9F8,
        ),
        borderRadius:
        BorderRadius.circular(18),
      ),
      child: Column(
        crossAxisAlignment:
        CrossAxisAlignment.start,
        children: [
          Text(
            'AI 분석 흐름',
            style: Theme.of(context)
                .textTheme
                .titleMedium,
          ),

          const SizedBox(height: 14),

          const _FlowRow(
            number: '1',
            text:
            'Flutter에서 사진을 선택하거나 촬영',
          ),

          const _FlowRow(
            number: '2',
            text:
            '모바일 AI가 품목을 1차 분석',
          ),

          const _FlowRow(
            number: '3',
            text:
            '신뢰도가 낮으면 서버 AI가 재분석',
          ),

          const _FlowRow(
            number: '4',
            text:
            '분리배출 가이드와 내 지역 일정 안내',
            last: true,
          ),
        ],
      ),
    );
  }
}

class _FlowRow extends StatelessWidget {
  const _FlowRow({
    required this.number,
    required this.text,
    this.last = false,
  });

  final String number;
  final String text;
  final bool last;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.only(
        bottom: last ? 0 : 12,
      ),
      child: Row(
        children: [
          Container(
            width: 26,
            height: 26,
            alignment:
            Alignment.center,
            decoration: BoxDecoration(
              color: AppTheme.primaryColor
                  .withValues(
                alpha: 0.1,
              ),
              shape: BoxShape.circle,
            ),
            child: Text(
              number,
              style: const TextStyle(
                color:
                AppTheme.primaryColor,
                fontWeight:
                FontWeight.w700,
                fontSize: 12,
              ),
            ),
          ),

          const SizedBox(width: 10),

          Expanded(
            child: Text(
              text,
              style: Theme.of(context)
                  .textTheme
                  .bodyMedium,
            ),
          ),
        ],
      ),
    );
  }
}