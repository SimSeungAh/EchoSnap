import 'package:flutter/material.dart';
import 'package:echosnap/core/theme/app_theme.dart';

class AiAnalysisResultCard
    extends StatelessWidget {
  const AiAnalysisResultCard({
    super.key,
    required this.wasteItemName,
    required this.confidence,
    required this.usedServerReanalysis,
    required this.userCorrected,
    required this.isCorrecting,
    required this.correctionSubmitted,
    required this.onAccept,
    required this.onCorrect,
    required this.onReport,
  });

  final String wasteItemName;

  final double? confidence;

  final bool usedServerReanalysis;

  /// AI 결과가 아니라
  /// 사용자가 직접 선택해 수정한 품목인지 여부입니다.
  final bool userCorrected;

  final bool isCorrecting;

  final bool correctionSubmitted;

  final VoidCallback onAccept;

  final VoidCallback onCorrect;

  final VoidCallback onReport;

  String get _confidenceText {
    final double? value =
        confidence;

    if (value == null) {
      return '';
    }

    return '${(value * 100).toStringAsFixed(1)}%';
  }

  @override
  Widget build(
      BuildContext context,
      ) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(
        20,
      ),
      decoration: BoxDecoration(
        color: AppTheme.primaryColor
            .withValues(
          alpha: 0.08,
        ),
        borderRadius:
        BorderRadius.circular(
          22,
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
                width: 48,
                height: 48,
                alignment:
                Alignment.center,
                decoration:
                BoxDecoration(
                  color: Colors.white,
                  borderRadius:
                  BorderRadius.circular(
                    15,
                  ),
                ),
                child: Icon(
                  userCorrected
                      ? Icons
                      .fact_check_outlined
                      : Icons
                      .auto_awesome_rounded,
                  color: AppTheme
                      .primaryColor,
                ),
              ),

              const SizedBox(
                width: 13,
              ),

              Expanded(
                child: Column(
                  crossAxisAlignment:
                  CrossAxisAlignment
                      .start,
                  children: [
                    Text(
                      userCorrected
                          ? '선택한 품목'
                          : 'AI 추정 결과',
                      style:
                      const TextStyle(
                        fontSize: 16,
                        fontWeight:
                        FontWeight.w800,
                      ),
                    ),

                    const SizedBox(
                      height: 3,
                    ),

                    Text(
                      userCorrected
                          ? 'AI 결과를 수정해 직접 선택한 품목이에요.'
                          : usedServerReanalysis
                          ? '정확도를 높이기 위해 서버 AI로 한 번 더 확인했어요.'
                          : '휴대폰에서 사진을 분석한 결과예요.',
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
            ],
          ),

          const SizedBox(
            height: 20,
          ),

          Container(
            width: double.infinity,
            padding:
            const EdgeInsets.symmetric(
              horizontal: 16,
              vertical: 17,
            ),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius:
              BorderRadius.circular(
                16,
              ),
            ),
            child: Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment:
                    CrossAxisAlignment
                        .start,
                    children: [
                      Text(
                        userCorrected
                            ? '확인 품목'
                            : '추정 품목',
                        style:
                        const TextStyle(
                          color: AppTheme
                              .textSecondaryColor,
                          fontSize: 12,
                        ),
                      ),

                      const SizedBox(
                        height: 5,
                      ),

                      Text(
                        wasteItemName,
                        style:
                        const TextStyle(
                          fontSize: 21,
                          fontWeight:
                          FontWeight.w800,
                        ),
                      ),
                    ],
                  ),
                ),

                if (!userCorrected &&
                    confidence != null)
                  Container(
                    padding:
                    const EdgeInsets.symmetric(
                      horizontal: 12,
                      vertical: 8,
                    ),
                    decoration:
                    BoxDecoration(
                      color: AppTheme
                          .primaryColor
                          .withValues(
                        alpha: 0.1,
                      ),
                      borderRadius:
                      BorderRadius.circular(
                        999,
                      ),
                    ),
                    child: Text(
                      _confidenceText,
                      style:
                      const TextStyle(
                        color: AppTheme
                            .primaryColor,
                        fontWeight:
                        FontWeight.w800,
                      ),
                    ),
                  ),
              ],
            ),
          ),

          const SizedBox(
            height: 14,
          ),

          Row(
            crossAxisAlignment:
            CrossAxisAlignment.start,
            children: [
              const Icon(
                Icons.info_outline_rounded,
                size: 17,
                color: AppTheme
                    .textSecondaryColor,
              ),

              const SizedBox(
                width: 7,
              ),

              Expanded(
                child: Text(
                  userCorrected
                      ? '선택한 품목을 기준으로 배출 방법을 안내할게요. '
                      '실제 재질과 오염 상태도 함께 확인해주세요.'
                      : 'AI는 사진을 바탕으로 품목을 추정해요. '
                      '결과가 실제 물건과 다르면 직접 수정할 수 있어요.',
                  style:
                  const TextStyle(
                    color: AppTheme
                        .textSecondaryColor,
                    fontSize: 12,
                    height: 1.5,
                  ),
                ),
              ),
            ],
          ),

          const SizedBox(
            height: 20,
          ),

          if (!userCorrected) ...[
            const Text(
              '이 결과가 맞나요?',
              style: TextStyle(
                fontSize: 15,
                fontWeight:
                FontWeight.w800,
              ),
            ),

            const SizedBox(
              height: 12,
            ),

            SizedBox(
              width: double.infinity,
              height: 50,
              child:
              ElevatedButton.icon(
                onPressed:
                isCorrecting
                    ? null
                    : onAccept,
                icon: const Icon(
                  Icons
                      .check_circle_outline_rounded,
                ),
                label: const Text(
                  '이 품목이 맞아요',
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
                    : onCorrect,
                style: OutlinedButton.styleFrom(
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(16),
                  ),
                ),
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
                      .edit_outlined,
                ),
                label: Text(
                  isCorrecting
                      ? '수정 결과 저장 중...'
                      : '품목 변경하기',
                ),
              ),
            ),
          ] else ...[
            SizedBox(
              width: double.infinity,
              height: 52,
              child:
              ElevatedButton.icon(
                onPressed:
                isCorrecting || correctionSubmitted
                    ? null
                    : onReport,
                icon: Icon(
                  correctionSubmitted
                      ? Icons.check_circle_rounded
                      : Icons.send_rounded,
                ),
                label: Text(
                  correctionSubmitted
                      ? '정정 요청을 보냈어요'
                      : '관리자에게 정정 보내기',
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
                    : onCorrect,
                style: OutlinedButton.styleFrom(
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(16),
                  ),
                ),
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
                      .edit_outlined,
                ),
                label: Text(
                  isCorrecting
                      ? '변경 결과 저장 중...'
                      : '품목 다시 변경하기',
                ),
              ),
            ),

            const SizedBox(
              height: 4,
            ),

            TextButton.icon(
              onPressed:
              isCorrecting
                  ? null
                  : onAccept,
              icon: const Icon(
                Icons.fact_check_outlined,
              ),
              label: const Text(
                '배출 전 확인하기',
              ),
            ),
          ],
        ],
      ),
    );
  }
}
