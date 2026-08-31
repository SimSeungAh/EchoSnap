import 'package:flutter/material.dart';
import 'package:smart_recycle/app/app_routes.dart';
import 'package:smart_recycle/core/theme/app_theme.dart';

class AiDisposalCheckPage
    extends StatefulWidget {
  const AiDisposalCheckPage({
    super.key,
    required this.wasteItemId,
    required this.wasteItemName,
    required this.confidence,
    required this.modelVersion,
  });

  final int wasteItemId;

  final String wasteItemName;

  final double? confidence;

  final String? modelVersion;

  @override
  State<AiDisposalCheckPage> createState() =>
      _AiDisposalCheckPageState();
}

class _AiDisposalCheckPageState
    extends State<AiDisposalCheckPage> {
  bool _materialChecked = false;

  bool _contaminationChecked = false;

  bool _separationChecked = false;

  bool get _allChecked {
    return _materialChecked &&
        _contaminationChecked &&
        _separationChecked;
  }

  void _openWasteDetail() {
    if (!_allChecked) {
      return;
    }

    Navigator.pushNamed(
      context,
      AppRoutes.wasteDetail,
      arguments: widget.wasteItemId,
    );
  }

  String _confidenceText() {
    final double? confidence =
        widget.confidence;

    if (confidence == null) {
      return '확인할 수 없음';
    }

    return '${(confidence * 100).toStringAsFixed(1)}%';
  }

  @override
  Widget build(
      BuildContext context,
      ) {
    return Scaffold(
      appBar: AppBar(
        title: const Text(
          '배출 전 확인',
        ),
      ),
      body: ListView(
        padding:
        const EdgeInsets.fromLTRB(
          20,
          16,
          20,
          40,
        ),
        children: [
          _buildAiResultCard(),

          const SizedBox(height: 22),

          Text(
            '실제 제품을 확인해주세요',
            style: Theme.of(context)
                .textTheme
                .titleLarge,
          ),

          const SizedBox(height: 8),

          Text(
            'AI는 사진에서 보이는 형태를 바탕으로 '
                '품목을 추정합니다. 실제 배출 방법은 '
                '제품의 재질, 오염 상태, 다른 재질의 '
                '분리 가능 여부에 따라 달라질 수 있어요.',
            style: Theme.of(context)
                .textTheme
                .bodyMedium
                ?.copyWith(
              height: 1.5,
            ),
          ),

          const SizedBox(height: 18),

          _CheckCard(
            value: _materialChecked,
            icon:
            Icons.sell_outlined,
            title:
            '제품의 재질·분리배출 표시를 확인했나요?',
            description:
            '용기 바닥이나 라벨 등에 표시된 '
                '재질과 분리배출 표시를 직접 확인해주세요.',
            onChanged: (value) {
              setState(() {
                _materialChecked =
                    value;
              });
            },
          ),

          const SizedBox(height: 12),

          _CheckCard(
            value: _contaminationChecked,
            icon:
            Icons.cleaning_services_outlined,
            title:
            '내용물과 오염 상태를 확인했나요?',
            description:
            '내용물이 많이 남아 있거나 '
                '깨끗하게 제거하기 어려우면 '
                '재활용이 어려울 수 있어요.',
            onChanged: (value) {
              setState(() {
                _contaminationChecked =
                    value;
              });
            },
          ),

          const SizedBox(height: 12),

          _CheckCard(
            value: _separationChecked,
            icon:
            Icons.call_split_rounded,
            title:
            '다른 재질의 부품을 분리할 수 있는지 확인했나요?',
            description:
            '뚜껑, 펌프, 라벨 등 다른 재질이 '
                '붙어 있다면 분리 가능 여부를 확인해주세요.',
            onChanged: (value) {
              setState(() {
                _separationChecked =
                    value;
              });
            },
          ),

          const SizedBox(height: 20),

          _buildNotice(),

          const SizedBox(height: 24),

          SizedBox(
            height: 52,
            child: ElevatedButton.icon(
              onPressed:
              _allChecked
                  ? _openWasteDetail
                  : null,
              icon: const Icon(
                Icons
                    .recycling_rounded,
              ),
              label: const Text(
                '품목별 배출 가이드 확인',
              ),
            ),
          ),

          const SizedBox(height: 10),

          TextButton(
            onPressed: () {
              Navigator.pop(
                context,
              );
            },
            child: const Text(
              '사진과 AI 결과 다시 보기',
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildAiResultCard() {
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
            alpha: 0.18,
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
                width: 46,
                height: 46,
                alignment:
                Alignment.center,
                decoration:
                BoxDecoration(
                  color: Colors.white,
                  borderRadius:
                  BorderRadius.circular(
                    14,
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

              const Expanded(
                child: Column(
                  crossAxisAlignment:
                  CrossAxisAlignment.start,
                  children: [
                    Text(
                      'AI 추정 결과',
                      style: TextStyle(
                        fontWeight:
                        FontWeight.w800,
                        fontSize: 16,
                      ),
                    ),

                    SizedBox(
                      height: 3,
                    ),

                    Text(
                      '확정된 재활용 판정이 아닙니다.',
                      style: TextStyle(
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
            height: 18,
          ),

          _InfoRow(
            label: '추정 품목',
            value:
            widget.wasteItemName,
          ),

          _InfoRow(
            label: 'AI 신뢰도',
            value:
            _confidenceText(),
          ),

          if (widget.modelVersion !=
              null)
            _InfoRow(
              label: '사용 모델',
              value:
              widget.modelVersion!,
            ),
        ],
      ),
    );
  }

  Widget _buildNotice() {
    return Container(
      padding:
      const EdgeInsets.all(
        16,
      ),
      decoration: BoxDecoration(
        color: const Color(
          0xFFFFF7E8,
        ),
        borderRadius:
        BorderRadius.circular(
          16,
        ),
      ),
      child: const Row(
        crossAxisAlignment:
        CrossAxisAlignment.start,
        children: [
          Icon(
            Icons
                .info_outline_rounded,
            size: 21,
          ),

          SizedBox(width: 10),

          Expanded(
            child: Text(
              '위 항목을 모두 확인해도 '
                  '재활용 가능 여부가 자동으로 '
                  '확정되는 것은 아닙니다. '
                  '다음 화면의 품목별 배출 방법과 '
                  '현재 거주지의 배출 기준을 함께 '
                  '확인해주세요.',
              style: TextStyle(
                height: 1.45,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _CheckCard
    extends StatelessWidget {
  const _CheckCard({
    required this.value,
    required this.icon,
    required this.title,
    required this.description,
    required this.onChanged,
  });

  final bool value;

  final IconData icon;

  final String title;

  final String description;

  final ValueChanged<bool> onChanged;

  @override
  Widget build(
      BuildContext context,
      ) {
    return InkWell(
      borderRadius:
      BorderRadius.circular(
        18,
      ),
      onTap: () {
        onChanged(
          !value,
        );
      },
      child: Container(
        padding:
        const EdgeInsets.all(
          16,
        ),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius:
          BorderRadius.circular(
            18,
          ),
          border: Border.all(
            color: value
                ? AppTheme.primaryColor
                .withValues(
              alpha: 0.45,
            )
                : const Color(
              0xFFDDE3E0,
            ),
          ),
        ),
        child: Row(
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
                color: AppTheme
                    .primaryColor
                    .withValues(
                  alpha: 0.08,
                ),
                borderRadius:
                BorderRadius.circular(
                  13,
                ),
              ),
              child: Icon(
                icon,
                color:
                AppTheme.primaryColor,
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
                  Text(
                    title,
                    style:
                    const TextStyle(
                      fontWeight:
                      FontWeight.w700,
                    ),
                  ),

                  const SizedBox(
                    height: 5,
                  ),

                  Text(
                    description,
                    style:
                    const TextStyle(
                      color: AppTheme
                          .textSecondaryColor,
                      fontSize: 12,
                      height: 1.4,
                    ),
                  ),
                ],
              ),
            ),

            const SizedBox(
              width: 8,
            ),

            Checkbox(
              value: value,
              onChanged: (
                  checked,
                  ) {
                onChanged(
                  checked ?? false,
                );
              },
            ),
          ],
        ),
      ),
    );
  }
}

class _InfoRow
    extends StatelessWidget {
  const _InfoRow({
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
            width: 88,
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
              style:
              const TextStyle(
                fontWeight:
                FontWeight.w600,
              ),
            ),
          ),
        ],
      ),
    );
  }
}