import 'package:flutter/material.dart';
import 'package:smart_recycle/app/app_routes.dart';
import 'package:smart_recycle/core/storage/token_storage.dart';
import 'package:smart_recycle/core/theme/app_theme.dart';
import 'package:smart_recycle/features/schedule/data/schedule_api.dart';
import 'package:smart_recycle/features/user/data/current_user_api.dart';

class SchedulePage extends StatefulWidget {
  const SchedulePage({
    super.key,
  });

  @override
  State<SchedulePage> createState() =>
      _SchedulePageState();
}

class _SchedulePageState
    extends State<SchedulePage> {
  CurrentUser? _user;

  GeneralHousingScheduleData?
  _generalHousingData;

  ManagedComplexScheduleData?
  _managedComplexData;

  bool _isLoading = true;

  String? _errorMessage;

  @override
  void initState() {
    super.initState();

    _load();
  }

  Future<void> _load() async {
    if (mounted) {
      setState(() {
        _isLoading = true;
        _errorMessage = null;
      });
    }

    try {
      final CurrentUser user =
      await CurrentUserApi.getMe();

      GeneralHousingScheduleData?
      generalHousingData;

      ManagedComplexScheduleData?
      managedComplexData;

      if (user.residenceType ==
          'GENERAL_HOUSING') {
        generalHousingData =
        await ScheduleApi
            .getGeneralHousingSchedule();
      } else if (user.residenceType ==
          'MANAGED_COMPLEX') {
        managedComplexData =
        await ScheduleApi
            .getManagedComplexSchedule();
      }

      if (!mounted) {
        return;
      }

      setState(() {
        _user = user;
        _generalHousingData =
            generalHousingData;
        _managedComplexData =
            managedComplexData;
        _isLoading = false;
      });
    } on CurrentUserApiException catch (
    exception
    ) {
      await _handleError(
        exception.message,
        unauthorized:
        exception.unauthorized,
      );
    } on ScheduleApiException catch (
    exception
    ) {
      await _handleError(
        exception.message,
        unauthorized:
        exception.unauthorized,
      );
    } catch (_) {
      await _handleError(
        '배출 일정을 불러오는 중 '
            '오류가 발생했습니다.',
      );
    }
  }

  Future<void> _handleError(
      String message, {
        bool unauthorized = false,
      }) async {
    if (!mounted) {
      return;
    }

    if (unauthorized) {
      await TokenStorage.clearTokens();

      if (!mounted) {
        return;
      }

      Navigator.pushNamedAndRemoveUntil(
        context,
        AppRoutes.login,
            (route) => false,
      );

      return;
    }

    setState(() {
      _isLoading = false;
      _errorMessage = message;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text(
          '배출 일정',
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
          padding:
          const EdgeInsets.all(32),
          child: Column(
            mainAxisSize:
            MainAxisSize.min,
            children: [
              const Icon(
                Icons.error_outline_rounded,
                size: 48,
                color:
                AppTheme.textSecondaryColor,
              ),

              const SizedBox(height: 16),

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

    final CurrentUser? user = _user;

    if (user == null ||
        user.residenceType == null) {
      return _buildResidenceRequired();
    }

    if (user.residenceType ==
        'GENERAL_HOUSING') {
      return _buildGeneralHousing();
    }

    if (user.residenceType ==
        'MANAGED_COMPLEX') {
      return _buildManagedComplex();
    }

    return _buildResidenceRequired();
  }

  Widget _buildResidenceRequired() {
    return Center(
      child: Padding(
        padding:
        const EdgeInsets.all(32),
        child: Column(
          mainAxisSize:
          MainAxisSize.min,
          children: [
            const Icon(
              Icons.home_outlined,
              size: 52,
              color:
              AppTheme.primaryColor,
            ),

            const SizedBox(height: 16),

            Text(
              '거주지 설정이 필요해요',
              style: Theme.of(context)
                  .textTheme
                  .titleLarge,
            ),

            const SizedBox(height: 8),

            Text(
              '주소나 공동주택을 설정하면 '
                  '내 거주지의 배출 일정을 '
                  '확인할 수 있어요.',
              textAlign:
              TextAlign.center,
            ),

            const SizedBox(height: 22),

            ElevatedButton(
              onPressed: () async {
                await Navigator.pushNamed(
                  context,
                  AppRoutes.residenceSetup,
                );

                if (mounted) {
                  await _load();
                }
              },
              child: const Text(
                '거주지 설정하기',
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildGeneralHousing() {
    final GeneralHousingScheduleData?
    data =
        _generalHousingData;

    if (data == null) {
      return const Center(
        child: Text(
          '지역 배출 일정이 없습니다.',
        ),
      );
    }

    return RefreshIndicator(
      onRefresh: _load,
      child: ListView(
        physics:
        const AlwaysScrollableScrollPhysics(),
        padding:
        const EdgeInsets.fromLTRB(
          20,
          12,
          20,
          40,
        ),
        children: [
          _LocationCard(
            title: '일반주택',
            address:
            data.addressName,
            subtitle: _buildDongText(
              data,
            ),
          ),

          const SizedBox(height: 26),

          Text(
            '내 지역 일정',
            style: Theme.of(context)
                .textTheme
                .titleLarge,
          ),

          const SizedBox(height: 12),

          if (data.schedules.isEmpty)
            const _EmptyCard(
              message:
              '등록된 지역 배출 일정이 없습니다.',
            )
          else
            ...data.schedules.map(
                  (schedule) {
                return Padding(
                  padding:
                  const EdgeInsets.only(
                    bottom: 12,
                  ),
                  child:
                  _GeneralScheduleCard(
                    schedule: schedule,
                  ),
                );
              },
            ),
        ],
      ),
    );
  }

  String _buildDongText(
      GeneralHousingScheduleData data,
      ) {
    final String? administrativeDong =
        data.administrativeDong;

    if (administrativeDong != null &&
        administrativeDong
            .trim()
            .isNotEmpty) {
      return '${data.sigungu} '
          '$administrativeDong';
    }

    final String? legalDong =
        data.legalDong;

    if (legalDong != null &&
        legalDong.trim().isNotEmpty) {
      return '${data.sigungu} '
          '$legalDong';
    }

    return '${data.sido} '
        '${data.sigungu}';
  }

  Widget _buildManagedComplex() {
    final ManagedComplexScheduleData?
    data =
        _managedComplexData;

    if (data == null) {
      return const Center(
        child: Text(
          '공동주택 배출 일정이 없습니다.',
        ),
      );
    }

    return RefreshIndicator(
      onRefresh: _load,
      child: ListView(
        physics:
        const AlwaysScrollableScrollPhysics(),
        padding:
        const EdgeInsets.fromLTRB(
          20,
          12,
          20,
          40,
        ),
        children: [
          _LocationCard(
            title: '공동주택',
            address:
            data.apartmentName,
            subtitle:
            '관리주체 공식 배출 일정',
          ),

          const SizedBox(height: 26),

          Text(
            '품목별 일정',
            style: Theme.of(context)
                .textTheme
                .titleLarge,
          ),

          const SizedBox(height: 12),

          if (data.items.isEmpty)
            const _EmptyCard(
              message:
              '등록된 공동주택 배출 일정이 없습니다.',
            )
          else
            ...data.items.map(
                  (item) {
                return Padding(
                  padding:
                  const EdgeInsets.only(
                    bottom: 12,
                  ),
                  child:
                  _ManagedScheduleCard(
                    item: item,
                  ),
                );
              },
            ),
        ],
      ),
    );
  }
}

class _LocationCard
    extends StatelessWidget {
  const _LocationCard({
    required this.title,
    required this.address,
    required this.subtitle,
  });

  final String title;
  final String address;
  final String subtitle;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding:
      const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: AppTheme.primaryColor
            .withValues(
          alpha: 0.08,
        ),
        borderRadius:
        BorderRadius.circular(20),
      ),
      child: Row(
        children: [
          Container(
            width: 48,
            height: 48,
            decoration:
            BoxDecoration(
              color: Colors.white,
              borderRadius:
              BorderRadius.circular(
                15,
              ),
            ),
            child: const Icon(
              Icons.location_on_outlined,
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
                  style: const TextStyle(
                    color: AppTheme
                        .primaryColor,
                    fontWeight:
                    FontWeight.w700,
                    fontSize: 13,
                  ),
                ),

                const SizedBox(height: 4),

                Text(
                  address,
                  style: Theme.of(context)
                      .textTheme
                      .titleMedium,
                ),

                const SizedBox(height: 3),

                Text(
                  subtitle,
                  style: Theme.of(context)
                      .textTheme
                      .bodyMedium,
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _GeneralScheduleCard
    extends StatelessWidget {
  const _GeneralScheduleCard({
    required this.schedule,
  });

  final GeneralHousingScheduleItem
  schedule;

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
            Row(
              children: [
                Container(
                  width: 44,
                  height: 44,
                  decoration:
                  BoxDecoration(
                    color: AppTheme
                        .primaryColor
                        .withValues(
                      alpha: 0.09,
                    ),
                    borderRadius:
                    BorderRadius.circular(
                      14,
                    ),
                  ),
                  child: Icon(
                    _iconForWasteType(
                      schedule.wasteType,
                    ),
                    color: AppTheme
                        .primaryColor,
                  ),
                ),

                const SizedBox(width: 12),

                Expanded(
                  child: Text(
                    schedule.wasteTypeLabel,
                    style:
                    Theme.of(context)
                        .textTheme
                        .titleMedium,
                  ),
                ),

                _StatusBadge(
                  availableNow:
                  schedule.availableNow,
                  availableToday:
                  schedule.availableToday,
                ),
              ],
            ),

            const SizedBox(height: 16),

            if (!schedule
                .collectionAreaMatched)
              const _InlineNotice(
                message:
                '현재 주소에 맞는 수거구역이 '
                    '아직 연결되지 않았습니다.',
              )
            else if (!schedule
                .scheduleAvailable &&
                schedule.todayException ==
                    null)
              const _InlineNotice(
                message:
                '수거구역은 연결됐지만 '
                    '등록된 공식 일정이 없습니다.',
              )
            else ...[
                if (_hasText(
                  schedule.collectionAreaName,
                ))
                  _ScheduleInfoRow(
                    label: '수거구역',
                    value: schedule
                        .collectionAreaName!,
                  ),

                if (_hasText(
                  schedule.targetAreaName,
                ))
                  _ScheduleInfoRow(
                    label: '적용지역',
                    value:
                    schedule.targetAreaName!,
                  ),

                if (_hasText(
                  schedule.emissionDays,
                ))
                  _ScheduleInfoRow(
                    label: '배출요일',
                    value:
                    schedule.emissionDays!,
                  ),

                if (schedule.startTime != null ||
                    schedule.endTime != null)
                  _ScheduleInfoRow(
                    label: '배출시간',
                    value: _timeRange(
                      schedule.startTime,
                      schedule.endTime,
                      schedule.overnight,
                    ),
                  ),

                if (_hasText(
                  schedule.emissionPlace,
                ))
                  _ScheduleInfoRow(
                    label: '배출장소',
                    value:
                    schedule.emissionPlace!,
                  ),

                if (_hasText(
                  schedule.emissionMethod,
                ))
                  _ScheduleInfoRow(
                    label: '배출방법',
                    value:
                    schedule.emissionMethod!,
                  ),

                if (_hasText(
                  schedule.uncollectedDay,
                ))
                  _ScheduleInfoRow(
                    label: '미수거일',
                    value:
                    schedule.uncollectedDay!,
                  ),

                if (schedule.todayException !=
                    null) ...[
                  const SizedBox(height: 14),

                  _ExceptionCard(
                    exception:
                    schedule.todayException!,
                  ),
                ],

                if (!schedule.availableToday &&
                    schedule.nextAvailableDate !=
                        null) ...[
                  const SizedBox(height: 14),

                  Text(
                    '다음 배출일 · '
                        '${_formatDate(schedule.nextAvailableDate!)}',
                    style: const TextStyle(
                      color: AppTheme
                          .primaryColor,
                      fontWeight:
                      FontWeight.w700,
                    ),
                  ),
                ],
              ],
          ],
        ),
      ),
    );
  }

  IconData _iconForWasteType(
      String value,
      ) {
    return switch (value) {
      'LIFE_WASTE' =>
      Icons.delete_outline_rounded,
      'FOOD_WASTE' =>
      Icons.restaurant_outlined,
      'RECYCLABLE' =>
      Icons.recycling_rounded,
      _ =>
      Icons.delete_outline_rounded,
    };
  }
}

class _ManagedScheduleCard
    extends StatelessWidget {
  const _ManagedScheduleCard({
    required this.item,
  });

  final ManagedScheduleItem item;

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
            Row(
              children: [
                Container(
                  width: 44,
                  height: 44,
                  decoration:
                  BoxDecoration(
                    color: AppTheme
                        .primaryColor
                        .withValues(
                      alpha: 0.09,
                    ),
                    borderRadius:
                    BorderRadius.circular(
                      14,
                    ),
                  ),
                  child: const Icon(
                    Icons.recycling_rounded,
                    color: AppTheme
                        .primaryColor,
                  ),
                ),

                const SizedBox(width: 12),

                Expanded(
                  child: Column(
                    crossAxisAlignment:
                    CrossAxisAlignment
                        .start,
                    children: [
                      Text(
                        item.name,
                        style:
                        Theme.of(context)
                            .textTheme
                            .titleMedium,
                      ),

                      if (item
                          .categoryName
                          .isNotEmpty)
                        Text(
                          item.categoryName,
                          style:
                          Theme.of(context)
                              .textTheme
                              .bodyMedium,
                        ),
                    ],
                  ),
                ),

                _StatusBadge(
                  availableNow:
                  item.availableNow,
                  availableToday:
                  item.availableToday,
                ),
              ],
            ),

            if (item.schedules.isNotEmpty) ...[
              const SizedBox(height: 16),

              ...item.schedules.map(
                    (schedule) {
                  return Padding(
                    padding:
                    const EdgeInsets.only(
                      bottom: 7,
                    ),
                    child: Row(
                      children: [
                        SizedBox(
                          width: 48,
                          child: Text(
                            schedule
                                .alwaysAvailable
                                ? '상시'
                                : _dayLabel(
                              schedule
                                  .dayOfWeek,
                            ),
                            style:
                            const TextStyle(
                              fontWeight:
                              FontWeight
                                  .w700,
                            ),
                          ),
                        ),

                        Expanded(
                          child: Text(
                            schedule
                                .alwaysAvailable
                                ? '언제든 배출 가능'
                                : _timeRange(
                              schedule
                                  .startTime,
                              schedule
                                  .endTime,
                              false,
                            ),
                          ),
                        ),
                      ],
                    ),
                  );
                },
              ),
            ],

            if (item.todayException !=
                null) ...[
              const SizedBox(height: 12),

              _ExceptionCard(
                exception:
                item.todayException!,
              ),
            ],

            if (!item.availableToday &&
                item.nextAvailableDate !=
                    null) ...[
              const SizedBox(height: 10),

              Text(
                '다음 배출일 · '
                    '${_formatDate(item.nextAvailableDate!)}',
                style: const TextStyle(
                  color:
                  AppTheme.primaryColor,
                  fontWeight:
                  FontWeight.w700,
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
}

class _StatusBadge
    extends StatelessWidget {
  const _StatusBadge({
    required this.availableNow,
    required this.availableToday,
  });

  final bool availableNow;
  final bool availableToday;

  @override
  Widget build(BuildContext context) {
    final String label;

    if (availableNow) {
      label = '지금 가능';
    } else if (availableToday) {
      label = '오늘 일정';
    } else {
      label = '오늘 없음';
    }

    return Container(
      padding:
      const EdgeInsets.symmetric(
        horizontal: 9,
        vertical: 5,
      ),
      decoration: BoxDecoration(
        color: AppTheme.primaryColor
            .withValues(
          alpha:
          availableNow ? 0.14 : 0.07,
        ),
        borderRadius:
        BorderRadius.circular(999),
      ),
      child: Text(
        label,
        style: TextStyle(
          color:
          AppTheme.primaryColor,
          fontWeight:
          FontWeight.w700,
          fontSize: 11,
        ),
      ),
    );
  }
}

class _ExceptionCard
    extends StatelessWidget {
  const _ExceptionCard({
    required this.exception,
  });

  final ScheduleExceptionInfo exception;

  @override
  Widget build(BuildContext context) {
    String message;

    if (exception.unavailable) {
      message =
      '오늘은 배출할 수 없습니다.';
    } else if (exception.alwaysAvailable ==
        true) {
      message =
      '오늘은 상시 배출 가능합니다.';
    } else {
      message =
      '오늘은 예외 일정이 적용됩니다.';

      if (exception.startTime != null ||
          exception.endTime != null) {
        message +=
        ' ${_timeRange(
          exception.startTime,
          exception.endTime,
          false,
        )}';
      }
    }

    return Container(
      width: double.infinity,
      padding:
      const EdgeInsets.all(13),
      decoration: BoxDecoration(
        color: const Color(
          0xFFFFF7E8,
        ),
        borderRadius:
        BorderRadius.circular(14),
      ),
      child: Column(
        crossAxisAlignment:
        CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Icon(
                Icons
                    .campaign_outlined,
                size: 19,
              ),

              const SizedBox(width: 8),

              Expanded(
                child: Text(
                  message,
                  style: const TextStyle(
                    fontWeight:
                    FontWeight.w700,
                  ),
                ),
              ),
            ],
          ),

          if (_hasText(
            exception.reason,
          )) ...[
            const SizedBox(height: 6),

            Text(
              exception.reason!,
            ),
          ],
        ],
      ),
    );
  }
}

class _ScheduleInfoRow
    extends StatelessWidget {
  const _ScheduleInfoRow({
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
        top: 9,
      ),
      child: Row(
        crossAxisAlignment:
        CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 70,
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

class _InlineNotice
    extends StatelessWidget {
  const _InlineNotice({
    required this.message,
  });

  final String message;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding:
      const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: const Color(
          0xFFF6F8F7,
        ),
        borderRadius:
        BorderRadius.circular(14),
      ),
      child: Text(
        message,
        style: Theme.of(context)
            .textTheme
            .bodyMedium,
      ),
    );
  }
}

class _EmptyCard
    extends StatelessWidget {
  const _EmptyCard({
    required this.message,
  });

  final String message;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding:
        const EdgeInsets.all(22),
        child: Row(
          children: [
            const Icon(
              Icons
                  .calendar_month_outlined,
              color:
              AppTheme.primaryColor,
            ),

            const SizedBox(width: 14),

            Expanded(
              child: Text(
                message,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

String _dayLabel(
    String? value,
    ) {
  return switch (value) {
    'MONDAY' => '월요일',
    'TUESDAY' => '화요일',
    'WEDNESDAY' => '수요일',
    'THURSDAY' => '목요일',
    'FRIDAY' => '금요일',
    'SATURDAY' => '토요일',
    'SUNDAY' => '일요일',
    _ => '-',
  };
}

String _timeRange(
    String? start,
    String? end,
    bool overnight,
    ) {
  final String startText =
  _formatTime(start);

  final String endText =
  _formatTime(end);

  String value =
      '$startText ~ $endText';

  if (overnight) {
    value += ' (익일)';
  }

  return value;
}

String _formatTime(
    String? value,
    ) {
  if (value == null ||
      value.isEmpty) {
    return '-';
  }

  final List<String> parts =
  value.split(':');

  if (parts.length < 2) {
    return value;
  }

  return '${parts[0]}:${parts[1]}';
}

String _formatDate(
    String value,
    ) {
  final DateTime? date =
  DateTime.tryParse(value);

  if (date == null) {
    return value;
  }

  return '${date.month}월 '
      '${date.day}일';
}

bool _hasText(
    String? value,
    ) {
  return value != null &&
      value.trim().isNotEmpty;
}