import 'package:flutter/material.dart';
import 'package:smart_recycle/app/app_routes.dart';
import 'package:smart_recycle/core/storage/token_storage.dart';
import 'package:smart_recycle/core/theme/app_theme.dart';
import 'package:smart_recycle/features/user/data/current_user_api.dart';
import 'package:smart_recycle/features/waste/data/waste_detail_api.dart';

class WasteDetailPage extends StatefulWidget {
  const WasteDetailPage({
    super.key,
    required this.wasteItemId,
  });

  final int wasteItemId;

  @override
  State<WasteDetailPage> createState() =>
      _WasteDetailPageState();
}

class _WasteDetailPageState
    extends State<WasteDetailPage> {
  WasteItemDetail? _detail;

  GeneralHousingSchedule?
  _generalHousingSchedule;

  bool _isLoading = true;

  String? _errorMessage;

  @override
  void initState() {
    super.initState();

    _load();
  }

  Future<void> _load() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      final CurrentUser user =
      await CurrentUserApi.getMe();

      final WasteItemDetail detail =
      await WasteDetailApi.getItemDetail(
        widget.wasteItemId,
      );

      GeneralHousingSchedule?
      generalHousingSchedule;

      if (user.residenceType ==
          'GENERAL_HOUSING') {
        final result =
        await WasteDetailApi
            .getGeneralHousingSchedule();

        generalHousingSchedule =
            _findGeneralHousingSchedule(
              detail,
              result,
            );
      }

      if (!mounted) {
        return;
      }

      setState(() {
        _detail = detail;
        _generalHousingSchedule =
            generalHousingSchedule;
      });
    } on CurrentUserApiException catch (
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
        _errorMessage =
            exception.message;
      });
    } on WasteDetailApiException catch (
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
        _errorMessage =
            exception.message;
      });
    } catch (_) {
      if (!mounted) {
        return;
      }

      setState(() {
        _errorMessage =
        '품목 상세 정보를 불러오는 중 '
            '오류가 발생했습니다.';
      });
    } finally {
      if (mounted) {
        setState(() {
          _isLoading = false;
        });
      }
    }
  }

  GeneralHousingSchedule?
  _findGeneralHousingSchedule(
      WasteItemDetail detail,
      GeneralHousingScheduleResult result,
      ) {
    /*
     * 현재 SmartRecycle의 일반주택 공공 일정은
     * LIFE_WASTE / FOOD_WASTE / RECYCLABLE
     * 세 종류로 관리합니다.
     *
     * 기존 폐기물 품목 카테고리 중 GENERAL은
     * 생활쓰레기 일정으로,
     * 그 외 재활용 카테고리는 RECYCLABLE 일정으로
     * 연결합니다.
     *
     * 이후 백엔드에 명시적인 매핑 테이블을 두면
     * 이 로직을 서버로 이동할 예정입니다.
     */
    final String targetWasteType =
    detail.category.code == 'GENERAL'
        ? 'LIFE_WASTE'
        : 'RECYCLABLE';

    for (final schedule
    in result.schedules) {
      if (schedule.wasteType ==
          targetWasteType) {
        return schedule;
      }
    }

    return null;
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

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text(
          '분리배출 정보',
        ),
      ),
      body: _buildBody(),
    );
  }

  Widget _buildBody() {
    if (_isLoading) {
      return const Center(
        child: CircularProgressIndicator(),
      );
    }

    if (_errorMessage != null) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(32),
          child: Column(
            mainAxisSize:
            MainAxisSize.min,
            children: [
              const Icon(
                Icons.error_outline_rounded,
                size: 46,
                color:
                AppTheme.textSecondaryColor,
              ),
              const SizedBox(height: 14),
              Text(
                _errorMessage!,
                textAlign:
                TextAlign.center,
              ),
              const SizedBox(height: 20),
              ElevatedButton(
                onPressed: _load,
                child: const Text(
                  '다시 시도',
                ),
              ),
            ],
          ),
        ),
      );
    }

    final WasteItemDetail? detail =
        _detail;

    if (detail == null) {
      return const Center(
        child: Text(
          '품목 정보를 찾을 수 없습니다.',
        ),
      );
    }

    return RefreshIndicator(
      onRefresh: _load,
      child: ListView(
        padding:
        const EdgeInsets.fromLTRB(
          20,
          16,
          20,
          40,
        ),
        children: [
          _buildHeader(detail),

          const SizedBox(height: 22),

          _buildGuide(detail),

          const SizedBox(height: 22),

          _buildSchedule(detail),
        ],
      ),
    );
  }

  Widget _buildHeader(
      WasteItemDetail detail,
      ) {
    return Card(
      child: Padding(
        padding:
        const EdgeInsets.all(20),
        child: Row(
          children: [
            Container(
              width: 72,
              height: 72,
              clipBehavior:
              Clip.antiAlias,
              decoration: BoxDecoration(
                color: AppTheme
                    .primaryColor
                    .withValues(
                  alpha: 0.08,
                ),
                borderRadius:
                BorderRadius.circular(
                  20,
                ),
              ),
              child: _buildImage(
                detail.imageUrl,
              ),
            ),

            const SizedBox(width: 18),

            Expanded(
              child: Column(
                crossAxisAlignment:
                CrossAxisAlignment
                    .start,
                children: [
                  Text(
                    detail.name,
                    style: Theme.of(
                      context,
                    )
                        .textTheme
                        .headlineSmall
                        ?.copyWith(
                      fontWeight:
                      FontWeight.w800,
                    ),
                  ),

                  const SizedBox(
                    height: 6,
                  ),

                  Container(
                    padding:
                    const EdgeInsets
                        .symmetric(
                      horizontal: 10,
                      vertical: 5,
                    ),
                    decoration:
                    BoxDecoration(
                      color: AppTheme
                          .primaryColor
                          .withValues(
                        alpha: 0.1,
                      ),
                      borderRadius:
                      BorderRadius
                          .circular(
                        999,
                      ),
                    ),
                    child: Text(
                      detail.category.name,
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
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildImage(
      String? imageUrl,
      ) {
    final String? url =
    imageUrl?.trim();

    if (url == null ||
        url.isEmpty) {
      return const Icon(
        Icons.recycling_rounded,
        size: 36,
        color:
        AppTheme.primaryColor,
      );
    }

    return Image.network(
      url,
      fit: BoxFit.cover,
      errorBuilder: (
          context,
          error,
          stackTrace,
          ) {
        return const Icon(
          Icons.recycling_rounded,
          size: 36,
          color:
          AppTheme.primaryColor,
        );
      },
    );
  }

  Widget _buildGuide(
      WasteItemDetail detail,
      ) {
    final WasteGuideDetail? guide =
        detail.guide;

    return Column(
      crossAxisAlignment:
      CrossAxisAlignment.start,
      children: [
        Text(
          '분리배출 방법',
          style: Theme.of(context)
              .textTheme
              .titleLarge,
        ),

        const SizedBox(height: 12),

        if (guide == null)
          const _InfoCard(
            icon:
            Icons.info_outline_rounded,
            title:
            '등록된 가이드가 없습니다.',
            description:
            '관리자가 분리배출 정보를 '
                '등록하면 이곳에 표시됩니다.',
          )
        else ...[
          if (_hasText(
            guide.summary,
          ))
            _GuideSection(
              icon:
              Icons.lightbulb_outline,
              title: '한눈에 보기',
              content:
              guide.summary!,
            ),

          if (_hasText(
            guide.disposalMethod,
          )) ...[
            const SizedBox(height: 10),
            _GuideSection(
              icon:
              Icons.recycling_rounded,
              title: '배출 방법',
              content:
              guide.disposalMethod!,
            ),
          ],

          if (_hasText(
            guide.caution,
          )) ...[
            const SizedBox(height: 10),
            _GuideSection(
              icon:
              Icons.warning_amber_rounded,
              title: '주의사항',
              content:
              guide.caution!,
            ),
          ],

          if (guide
              .checkItems
              .isNotEmpty) ...[
            const SizedBox(height: 18),

            Text(
              '버리기 전 체크',
              style: Theme.of(context)
                  .textTheme
                  .titleMedium,
            ),

            const SizedBox(height: 10),

            ...guide.checkItems.map(
                  (item) {
                return Padding(
                  padding:
                  const EdgeInsets
                      .only(
                    bottom: 8,
                  ),
                  child:
                  _CheckItemCard(
                    item: item,
                  ),
                );
              },
            ),
          ],
        ],
      ],
    );
  }

  Widget _buildSchedule(
      WasteItemDetail detail,
      ) {
    final GeneralHousingSchedule?
    general =
        _generalHousingSchedule;

    /*
     * 일반주택 일정이 조회된 경우
     */
    if (general != null) {
      return Column(
        crossAxisAlignment:
        CrossAxisAlignment.start,
        children: [
          Text(
            '내 지역 배출 일정',
            style: Theme.of(context)
                .textTheme
                .titleLarge,
          ),

          const SizedBox(height: 12),

          _ScheduleStatusCard(
            availableToday:
            general.availableToday,
            availableNow:
            general.availableNow,
            nextAvailableDate:
            general.nextAvailableDate,
          ),

          const SizedBox(height: 10),

          _GeneralHousingScheduleCard(
            schedule: general,
          ),
        ],
      );
    }

    /*
     * 공동주택 품목별 일정
     */
    if (detail.schedules.isNotEmpty ||
        detail.availableToday ||
        detail.nextAvailableDate != null) {
      return Column(
        crossAxisAlignment:
        CrossAxisAlignment.start,
        children: [
          Text(
            '내 거주지 배출 일정',
            style: Theme.of(context)
                .textTheme
                .titleLarge,
          ),

          const SizedBox(height: 12),

          _ScheduleStatusCard(
            availableToday:
            detail.availableToday,
            availableNow:
            detail.availableNow,
            nextAvailableDate:
            detail.nextAvailableDate,
          ),

          if (detail
              .schedules
              .isNotEmpty) ...[
            const SizedBox(height: 10),

            Card(
              child: Padding(
                padding:
                const EdgeInsets.all(
                  18,
                ),
                child: Column(
                  children:
                  detail.schedules
                      .map(
                        (schedule) {
                      return Padding(
                        padding:
                        const EdgeInsets
                            .symmetric(
                          vertical: 7,
                        ),
                        child:
                        _ManagedScheduleRow(
                          schedule:
                          schedule,
                        ),
                      );
                    },
                  ).toList(),
                ),
              ),
            ),
          ],
        ],
      );
    }

    return Column(
      crossAxisAlignment:
      CrossAxisAlignment.start,
      children: [
        Text(
          '내 거주지 배출 일정',
          style: Theme.of(context)
              .textTheme
              .titleLarge,
        ),

        const SizedBox(height: 12),

        const _InfoCard(
          icon:
          Icons.calendar_month_outlined,
          title:
          '등록된 배출 일정이 없습니다.',
          description:
          '현재 거주지에 연결된 '
              '공식 일정이 아직 없습니다.',
        ),
      ],
    );
  }

  bool _hasText(
      String? value,
      ) {
    return value != null &&
        value.trim().isNotEmpty;
  }
}

class _GuideSection
    extends StatelessWidget {
  const _GuideSection({
    required this.icon,
    required this.title,
    required this.content,
  });

  final IconData icon;
  final String title;
  final String content;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding:
        const EdgeInsets.all(18),
        child: Row(
          crossAxisAlignment:
          CrossAxisAlignment.start,
          children: [
            Icon(
              icon,
              color:
              AppTheme.primaryColor,
            ),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment:
                CrossAxisAlignment.start,
                children: [
                  Text(
                    title,
                    style: Theme.of(
                      context,
                    )
                        .textTheme
                        .titleMedium,
                  ),
                  const SizedBox(
                    height: 7,
                  ),
                  Text(
                    content,
                    style: Theme.of(
                      context,
                    )
                        .textTheme
                        .bodyMedium
                        ?.copyWith(
                      height: 1.55,
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _CheckItemCard
    extends StatelessWidget {
  const _CheckItemCard({
    required this.item,
  });

  final WasteGuideCheckItem item;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding:
      const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: const Color(
          0xFFF6F8F7,
        ),
        borderRadius:
        BorderRadius.circular(14),
      ),
      child: Row(
        children: [
          Icon(
            item.requiredItem
                ? Icons
                .check_circle_rounded
                : Icons
                .check_circle_outline_rounded,
            size: 21,
            color:
            AppTheme.primaryColor,
          ),
          const SizedBox(width: 11),
          Expanded(
            child: Text(
              item.content,
            ),
          ),
        ],
      ),
    );
  }
}

class _ScheduleStatusCard
    extends StatelessWidget {
  const _ScheduleStatusCard({
    required this.availableToday,
    required this.availableNow,
    required this.nextAvailableDate,
  });

  final bool availableToday;
  final bool availableNow;
  final String? nextAvailableDate;

  @override
  Widget build(BuildContext context) {
    String title;
    String description;
    IconData icon;

    if (availableNow) {
      title = '지금 배출할 수 있어요';
      description =
      '현재 배출 가능한 시간입니다.';
      icon =
          Icons.check_circle_rounded;
    } else if (availableToday) {
      title = '오늘 배출 일정이 있어요';
      description =
      '배출 가능 시간을 확인해주세요.';
      icon =
          Icons.schedule_rounded;
    } else {
      title = '지금은 배출 시간이 아니에요';

      if (nextAvailableDate != null) {
        description =
        '다음 배출일: '
            '${_formatDate(nextAvailableDate!)}';
      } else {
        description =
        '다음 배출 일정이 아직 없습니다.';
      }

      icon =
          Icons.event_outlined;
    }

    return Card(
      child: Padding(
        padding:
        const EdgeInsets.all(18),
        child: Row(
          children: [
            Container(
              width: 48,
              height: 48,
              decoration:
              BoxDecoration(
                color: AppTheme
                    .primaryColor
                    .withValues(
                  alpha: 0.1,
                ),
                borderRadius:
                BorderRadius.circular(
                  15,
                ),
              ),
              child: Icon(
                icon,
                color:
                AppTheme.primaryColor,
              ),
            ),

            const SizedBox(width: 14),

            Expanded(
              child: Column(
                crossAxisAlignment:
                CrossAxisAlignment.start,
                children: [
                  Text(
                    title,
                    style: Theme.of(
                      context,
                    )
                        .textTheme
                        .titleMedium,
                  ),
                  const SizedBox(
                    height: 4,
                  ),
                  Text(
                    description,
                    style: Theme.of(
                      context,
                    )
                        .textTheme
                        .bodyMedium,
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  static String _formatDate(
      String value,
      ) {
    final List<String> parts =
    value.split('-');

    if (parts.length != 3) {
      return value;
    }

    return '${parts[0]}.${parts[1]}.${parts[2]}';
  }
}

class _GeneralHousingScheduleCard
    extends StatelessWidget {
  const _GeneralHousingScheduleCard({
    required this.schedule,
  });

  final GeneralHousingSchedule schedule;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding:
        const EdgeInsets.all(18),
        child: Column(
          crossAxisAlignment:
          CrossAxisAlignment.start,
          children: [
            Text(
              schedule.wasteTypeLabel,
              style: Theme.of(context)
                  .textTheme
                  .titleMedium,
            ),

            if (schedule
                .collectionAreaName !=
                null) ...[
              const SizedBox(height: 10),
              _InfoRow(
                label: '수거구역',
                value: schedule
                    .collectionAreaName!,
              ),
            ],

            if (schedule.emissionDays !=
                null)
              _InfoRow(
                label: '배출요일',
                value:
                schedule.emissionDays!,
              ),

            if (schedule.startTime != null &&
                schedule.endTime != null)
              _InfoRow(
                label: '배출시간',
                value:
                '${_formatTime(schedule.startTime!)}'
                    ' ~ '
                    '${_formatTime(schedule.endTime!)}',
              ),

            if (schedule.emissionPlace !=
                null)
              _InfoRow(
                label: '배출장소',
                value:
                schedule.emissionPlace!,
              ),

            if (schedule.emissionMethod !=
                null)
              _InfoRow(
                label: '배출방법',
                value:
                schedule.emissionMethod!,
              ),

            if (!schedule
                .collectionAreaMatched) ...[
              const SizedBox(height: 8),
              const Text(
                '현재 주소에 맞는 수거구역이 '
                    '아직 연결되지 않았습니다.',
              ),
            ] else if (!schedule
                .scheduleAvailable) ...[
              const SizedBox(height: 8),
              const Text(
                '수거구역은 확인됐지만 '
                    '공식 일정이 아직 없습니다.',
              ),
            ],
          ],
        ),
      ),
    );
  }

  String _formatTime(
      String value,
      ) {
    final parts =
    value.split(':');

    if (parts.length < 2) {
      return value;
    }

    return '${parts[0]}:${parts[1]}';
  }
}

class _ManagedScheduleRow
    extends StatelessWidget {
  const _ManagedScheduleRow({
    required this.schedule,
  });

  final ManagedComplexScheduleTime
  schedule;

  @override
  Widget build(BuildContext context) {
    if (schedule.alwaysAvailable) {
      return const Row(
        children: [
          Icon(
            Icons.all_inclusive_rounded,
            size: 20,
            color:
            AppTheme.primaryColor,
          ),
          SizedBox(width: 10),
          Text(
            '상시 배출 가능',
          ),
        ],
      );
    }

    return Row(
      children: [
        SizedBox(
          width: 38,
          child: Text(
            _dayLabel(
              schedule.dayOfWeek,
            ),
            style: const TextStyle(
              fontWeight:
              FontWeight.w700,
            ),
          ),
        ),
        Expanded(
          child: Text(
            '${_time(schedule.startTime)}'
                ' ~ '
                '${_time(schedule.endTime)}',
          ),
        ),
      ],
    );
  }

  String _dayLabel(
      String? value,
      ) {
    return switch (value) {
      'MONDAY' => '월',
      'TUESDAY' => '화',
      'WEDNESDAY' => '수',
      'THURSDAY' => '목',
      'FRIDAY' => '금',
      'SATURDAY' => '토',
      'SUNDAY' => '일',
      _ => '-',
    };
  }

  String _time(
      String? value,
      ) {
    if (value == null) {
      return '-';
    }

    final parts =
    value.split(':');

    if (parts.length < 2) {
      return value;
    }

    return '${parts[0]}:${parts[1]}';
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
  Widget build(BuildContext context) {
    return Padding(
      padding:
      const EdgeInsets.only(
        top: 10,
      ),
      child: Row(
        crossAxisAlignment:
        CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 72,
            child: Text(
              label,
              style: const TextStyle(
                color: AppTheme
                    .textSecondaryColor,
                fontSize: 13,
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

class _InfoCard
    extends StatelessWidget {
  const _InfoCard({
    required this.icon,
    required this.title,
    required this.description,
  });

  final IconData icon;
  final String title;
  final String description;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding:
        const EdgeInsets.all(18),
        child: Row(
          children: [
            Icon(
              icon,
              color:
              AppTheme.primaryColor,
            ),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment:
                CrossAxisAlignment.start,
                children: [
                  Text(
                    title,
                    style: Theme.of(
                      context,
                    )
                        .textTheme
                        .titleMedium,
                  ),
                  const SizedBox(
                    height: 5,
                  ),
                  Text(
                    description,
                    style: Theme.of(
                      context,
                    )
                        .textTheme
                        .bodyMedium,
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}