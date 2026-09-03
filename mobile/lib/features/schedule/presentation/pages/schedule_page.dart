import 'package:flutter/material.dart';
import 'package:echosnap/app/app_routes.dart';
import 'package:echosnap/core/storage/token_storage.dart';
import 'package:echosnap/core/theme/app_theme.dart';
import 'package:echosnap/features/schedule/data/schedule_api.dart';
import 'package:echosnap/features/user/data/current_user_api.dart';
import 'package:echosnap/features/waste/data/waste_search_api.dart';
import 'package:echosnap/features/waste/presentation/pages/waste_search_page.dart';

class SchedulePage extends StatefulWidget {
  const SchedulePage({super.key});

  @override
  State<SchedulePage> createState() => _SchedulePageState();
}

class _SchedulePageState extends State<SchedulePage> {
  CurrentUser? _user;

  GeneralHousingScheduleData? _generalHousingData;

  ManagedComplexScheduleData? _managedComplexData;
  List<ConfirmableScheduleReport> _confirmableReports = const [];
  List<ConfirmableScheduleReport> _myReports = const [];

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
      final CurrentUser user = await CurrentUserApi.getMe();

      GeneralHousingScheduleData? generalHousingData;

      ManagedComplexScheduleData? managedComplexData;
      List<ConfirmableScheduleReport> confirmableReports = const [];
      List<ConfirmableScheduleReport> myReports = const [];

      if (user.residenceType == 'GENERAL_HOUSING') {
        generalHousingData = await ScheduleApi.getGeneralHousingSchedule();
      } else if (user.residenceType == 'MANAGED_COMPLEX') {
        managedComplexData = await ScheduleApi.getManagedComplexSchedule();
        confirmableReports = await ScheduleApi.getConfirmableReports();
        myReports = await ScheduleApi.getMyReports();
      }

      if (!mounted) {
        return;
      }

      setState(() {
        _user = user;
        _generalHousingData = generalHousingData;
        _managedComplexData = managedComplexData;
        _confirmableReports = confirmableReports;
        _myReports = myReports;
        _isLoading = false;
      });
    } on CurrentUserApiException catch (exception) {
      await _handleError(
        exception.message,
        unauthorized: exception.unauthorized,
      );
    } on ScheduleApiException catch (exception) {
      await _handleError(
        exception.message,
        unauthorized: exception.unauthorized,
      );
    } catch (_) {
      await _handleError(
        '배출 일정을 불러오는 중 '
        '오류가 발생했습니다.',
      );
    }
  }

  Future<void> _handleError(String message, {bool unauthorized = false}) async {
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

  Future<void> _reportManagedSchedule({ManagedScheduleItem? existing}) async {
    final WasteSearchItem? selectedWasteItem = existing == null
        ? await Navigator.push<WasteSearchItem>(
            context,
            MaterialPageRoute(
              builder: (_) => const WasteSearchPage(selectionMode: true),
            ),
          )
        : null;
    if ((existing == null && selectedWasteItem == null) || !mounted) return;
    final int wasteItemId = existing?.wasteItemId ?? selectedWasteItem!.id;
    final String wasteItemName = existing?.name ?? selectedWasteItem!.name;

    if (existing == null) {
      for (final item
          in _managedComplexData?.items ?? const <ManagedScheduleItem>[]) {
        if (item.wasteItemId == wasteItemId) {
          existing = item;
          break;
        }
      }
    }
    final ManagedScheduleTime? current = existing?.schedules.isEmpty ?? true
        ? null
        : existing!.schedules.first;
    final _ScheduleReportDraft? draft = await showDialog<_ScheduleReportDraft>(
      context: context,
      builder: (_) =>
          _ScheduleReportDialog(wasteItemName: wasteItemName, current: current),
    );

    if (draft == null || !mounted) return;
    try {
      await ScheduleApi.reportApartmentSchedule(
        wasteItemId: wasteItemId,
        dayOfWeek: draft.day,
        startTime: draft.startTime,
        endTime: draft.endTime,
        alwaysAvailable: draft.always,
        referenceScheduleId: existing == null || existing.schedules.isEmpty
            ? null
            : existing.schedules.first.scheduleId,
        note: draft.note,
      );
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('일정 제보를 저장했어요. 같은 단지 주민이 확인할 수 있습니다.')),
        );
        await _load();
      }
    } on ScheduleApiException catch (exception) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(exception.message)));
      }
    }
  }

  Future<void> _confirmScheduleReport(
    ConfirmableScheduleReport report,
    String value,
  ) async {
    try {
      await ScheduleApi.confirmReport(report.id, value);
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(const SnackBar(content: Text('이웃 확인을 저장했어요.')));
        await _load();
      }
    } on ScheduleApiException catch (exception) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(exception.message)));
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('배출 일정'),
        actions: [
          if (_user?.residenceType == 'MANAGED_COMPLEX')
            TextButton.icon(
              onPressed: () => _reportManagedSchedule(),
              icon: const Icon(Icons.add_rounded),
              label: const Text('일정 제보'),
            ),
        ],
      ),
      body: _buildBody(),
    );
  }

  Widget _buildBody() {
    if (_isLoading) {
      return const Center(child: CircularProgressIndicator());
    }

    if (_errorMessage != null) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(32),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(
                Icons.error_outline_rounded,
                size: 48,
                color: AppTheme.textSecondaryColor,
              ),

              const SizedBox(height: 16),

              Text(_errorMessage!, textAlign: TextAlign.center),

              const SizedBox(height: 20),

              ElevatedButton(onPressed: _load, child: const Text('다시 시도')),
            ],
          ),
        ),
      );
    }

    final CurrentUser? user = _user;

    if (user == null || user.residenceType == null) {
      return _buildResidenceRequired();
    }

    if (user.residenceType == 'GENERAL_HOUSING') {
      return _buildGeneralHousing();
    }

    if (user.residenceType == 'MANAGED_COMPLEX') {
      return _buildManagedComplex();
    }

    return _buildResidenceRequired();
  }

  Widget _buildResidenceRequired() {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(
              Icons.home_outlined,
              size: 52,
              color: AppTheme.primaryColor,
            ),

            const SizedBox(height: 16),

            Text('거주지 설정이 필요해요', style: Theme.of(context).textTheme.titleLarge),

            const SizedBox(height: 8),

            Text(
              '주소나 공동주택을 설정하면 '
              '내 거주지의 배출 일정을 '
              '확인할 수 있어요.',
              textAlign: TextAlign.center,
            ),

            const SizedBox(height: 22),

            ElevatedButton(
              onPressed: () async {
                await Navigator.pushNamed(context, AppRoutes.residenceSetup);

                if (mounted) {
                  await _load();
                }
              },
              child: const Text('거주지 설정하기'),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildGeneralHousing() {
    final GeneralHousingScheduleData? data = _generalHousingData;

    if (data == null) {
      return const Center(child: Text('지역 배출 일정이 없습니다.'));
    }

    return RefreshIndicator(
      onRefresh: _load,
      child: ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.fromLTRB(20, 12, 20, 40),
        children: [
          _LocationCard(
            title: '일반주택',
            address: data.addressName,
            subtitle: _buildDongText(data),
          ),

          const SizedBox(height: 26),

          Text('내 지역 일정', style: Theme.of(context).textTheme.titleLarge),

          const SizedBox(height: 12),

          if (data.schedules.isEmpty)
            const _EmptyCard(message: '등록된 지역 배출 일정이 없습니다.')
          else
            ...data.schedules.map((schedule) {
              return Padding(
                padding: const EdgeInsets.only(bottom: 12),
                child: _GeneralScheduleCard(schedule: schedule),
              );
            }),
        ],
      ),
    );
  }

  String _buildDongText(GeneralHousingScheduleData data) {
    final String? administrativeDong = data.administrativeDong;

    if (administrativeDong != null && administrativeDong.trim().isNotEmpty) {
      return '${data.sigungu} '
          '$administrativeDong';
    }

    final String? legalDong = data.legalDong;

    if (legalDong != null && legalDong.trim().isNotEmpty) {
      return '${data.sigungu} '
          '$legalDong';
    }

    return '${data.sido} '
        '${data.sigungu}';
  }

  Widget _buildManagedComplex() {
    final ManagedComplexScheduleData? data = _managedComplexData;

    if (data == null) {
      return const Center(child: Text('공동주택 배출 일정이 없습니다.'));
    }

    return RefreshIndicator(
      onRefresh: _load,
      child: ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.fromLTRB(20, 12, 20, 40),
        children: [
          _LocationCard(
            title: '공동주택',
            address: data.apartmentName,
            subtitle: '관리자 승인 일정과 주민 제보를 함께 확인해요',
          ),

          if (data.items.isEmpty) ...[
            const SizedBox(height: 18),
            _FirstScheduleCard(onPressed: () => _reportManagedSchedule()),
          ],

          if (_myReports.isNotEmpty) ...[
            const SizedBox(height: 26),
            Text('내가 등록한 일정', style: Theme.of(context).textTheme.titleLarge),
            const SizedBox(height: 6),
            const Text('승인 전에도 같은 단지 주민에게 공유되어 확인을 받을 수 있어요.'),
            const SizedBox(height: 12),
            ..._myReports.map(
              (report) => Padding(
                padding: const EdgeInsets.only(bottom: 12),
                child: _MyScheduleReportCard(report: report),
              ),
            ),
          ],

          if (_confirmableReports.isNotEmpty) ...[
            const SizedBox(height: 26),
            Text('이웃 주민의 새 제보', style: Theme.of(context).textTheme.titleLarge),
            const SizedBox(height: 6),
            const Text('같은 단지 주민들이 확인하면 더 정확한 일정이 됩니다.'),
            const SizedBox(height: 12),
            ..._confirmableReports.map(
              (report) => Padding(
                padding: const EdgeInsets.only(bottom: 12),
                child: _ResidentReportCard(
                  report: report,
                  onConfirm: (value) => _confirmScheduleReport(report, value),
                ),
              ),
            ),
          ],

          const SizedBox(height: 26),

          Text('품목별 일정', style: Theme.of(context).textTheme.titleLarge),

          const SizedBox(height: 12),

          if (data.items.isEmpty)
            const _EmptyCard(message: '아직 승인된 일정이 없습니다. 주민 제보를 기다리고 있어요.')
          else
            ...data.items.map((item) {
              return Padding(
                padding: const EdgeInsets.only(bottom: 12),
                child: _ManagedScheduleCard(
                  item: item,
                  onCorrection: () => _reportManagedSchedule(existing: item),
                ),
              );
            }),
        ],
      ),
    );
  }
}

class _ScheduleReportDraft {
  const _ScheduleReportDraft({
    required this.day,
    required this.startTime,
    required this.endTime,
    required this.always,
    required this.note,
  });

  final String day;
  final String startTime;
  final String endTime;
  final bool always;
  final String note;
}

class _ScheduleReportDialog extends StatefulWidget {
  const _ScheduleReportDialog({required this.wasteItemName, this.current});

  final String wasteItemName;
  final ManagedScheduleTime? current;

  @override
  State<_ScheduleReportDialog> createState() => _ScheduleReportDialogState();
}

class _ScheduleReportDialogState extends State<_ScheduleReportDialog> {
  late String _day;
  late bool _always;
  late final TextEditingController _startController;
  late final TextEditingController _endController;
  final TextEditingController _placeController = TextEditingController();
  final TextEditingController _noteController = TextEditingController();

  @override
  void initState() {
    super.initState();
    _day = widget.current?.dayOfWeek ?? 'MONDAY';
    _always = widget.current?.alwaysAvailable ?? false;
    _startController = TextEditingController(
      text: widget.current?.startTime ?? '18:00',
    );
    _endController = TextEditingController(
      text: widget.current?.endTime ?? '21:00',
    );
  }

  @override
  void dispose() {
    _startController.dispose();
    _endController.dispose();
    _placeController.dispose();
    _noteController.dispose();
    super.dispose();
  }

  void _submit() {
    final place = _placeController.text.trim();
    final detail = _noteController.text.trim();
    final note = [
      if (place.isNotEmpty) '배출장소: $place',
      if (detail.isNotEmpty) detail,
    ].join('\n');
    Navigator.pop(
      context,
      _ScheduleReportDraft(
        day: _day,
        startTime: _startController.text.trim(),
        endTime: _endController.text.trim(),
        always: _always,
        note: note,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: Text(
        '${widget.wasteItemName} 일정 ${widget.current == null ? '등록' : '정정 제보'}',
      ),
      content: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            if (widget.current != null)
              const Padding(
                padding: EdgeInsets.only(bottom: 12),
                child: Text('실제 일정이 다르다면 올바른 요일과 시간을 알려주세요.'),
              ),
            SwitchListTile(
              contentPadding: EdgeInsets.zero,
              title: const Text('상시 배출 가능'),
              value: _always,
              onChanged: (value) => setState(() => _always = value),
            ),
            if (!_always) ...[
              DropdownButtonFormField<String>(
                initialValue: _day,
                decoration: const InputDecoration(labelText: '배출 요일'),
                items:
                    const [
                          ('MONDAY', '월요일'),
                          ('TUESDAY', '화요일'),
                          ('WEDNESDAY', '수요일'),
                          ('THURSDAY', '목요일'),
                          ('FRIDAY', '금요일'),
                          ('SATURDAY', '토요일'),
                          ('SUNDAY', '일요일'),
                        ]
                        .map(
                          (entry) => DropdownMenuItem(
                            value: entry.$1,
                            child: Text(entry.$2),
                          ),
                        )
                        .toList(),
                onChanged: (value) => setState(() => _day = value ?? _day),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: _startController,
                decoration: const InputDecoration(
                  labelText: '시작 시간 (예: 18:00)',
                ),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: _endController,
                decoration: const InputDecoration(
                  labelText: '종료 시간 (예: 21:00)',
                ),
              ),
            ],
            const SizedBox(height: 12),
            TextField(
              controller: _placeController,
              decoration: const InputDecoration(
                labelText: '배출장소 (선택)',
                hintText: '예: 101동 뒤 분리수거장',
              ),
            ),
            const SizedBox(height: 12),
            TextField(
              controller: _noteController,
              maxLength: 900,
              maxLines: 3,
              decoration: const InputDecoration(
                labelText: '근거·메모 (선택)',
                hintText: '관리사무소 공지 등 확인한 내용을 적어주세요.',
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
        ElevatedButton(onPressed: _submit, child: const Text('공유하기')),
      ],
    );
  }
}

class _FirstScheduleCard extends StatelessWidget {
  const _FirstScheduleCard({required this.onPressed});
  final VoidCallback onPressed;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          children: [
            const Icon(
              Icons.add_home_work_outlined,
              size: 42,
              color: AppTheme.primaryColor,
            ),
            const SizedBox(height: 10),
            Text(
              '우리 단지 첫 일정을 알려주세요',
              style: Theme.of(context).textTheme.titleMedium,
            ),
            const SizedBox(height: 6),
            const Text(
              '요일·시간·배출장소를 등록하면 같은 단지 주민들과 바로 공유됩니다.',
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 16),
            SizedBox(
              width: double.infinity,
              child: ElevatedButton.icon(
                onPressed: onPressed,
                icon: const Icon(Icons.add_rounded),
                label: const Text('첫 일정 등록하기'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _MyScheduleReportCard extends StatelessWidget {
  const _MyScheduleReportCard({required this.report});
  final ConfirmableScheduleReport report;

  @override
  Widget build(BuildContext context) {
    final schedule = report.alwaysAvailable
        ? '상시 배출 가능'
        : '${_dayLabel(report.dayOfWeek)} · ${_timeRange(report.startTime, report.endTime, false)}';
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Text(
                    report.wasteItemName,
                    style: Theme.of(context).textTheme.titleMedium,
                  ),
                ),
                _ReportTrustBadge(report: report),
              ],
            ),
            const SizedBox(height: 8),
            Text(schedule),
            if (_hasText(report.note)) ...[
              const SizedBox(height: 6),
              Text(report.note!),
            ],
            const SizedBox(height: 9),
            Text(
              '맞아요 ${report.confirmedCount} · 달라요 ${report.differentCount}',
              style: Theme.of(context).textTheme.bodySmall,
            ),
          ],
        ),
      ),
    );
  }
}

class _ReportTrustBadge extends StatelessWidget {
  const _ReportTrustBadge({required this.report});
  final ConfirmableScheduleReport report;

  @override
  Widget build(BuildContext context) {
    late final String label;
    late final Color color;
    if (report.status == 'APPROVED') {
      label = '관리자 확인';
      color = AppTheme.primaryColor;
    } else if (report.differentCount > report.confirmedCount) {
      label = '확인 필요';
      color = Colors.orange.shade800;
    } else if (report.confirmedCount >= 2) {
      label = '주민 확인';
      color = Colors.blue.shade700;
    } else {
      label = '확인 전';
      color = Colors.grey.shade700;
    }
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 5),
      decoration: BoxDecoration(
        color: color.withValues(alpha: .1),
        borderRadius: BorderRadius.circular(99),
      ),
      child: Text(
        label,
        style: TextStyle(
          color: color,
          fontSize: 11,
          fontWeight: FontWeight.w700,
        ),
      ),
    );
  }
}

class _ResidentReportCard extends StatelessWidget {
  const _ResidentReportCard({required this.report, required this.onConfirm});

  final ConfirmableScheduleReport report;
  final ValueChanged<String> onConfirm;

  @override
  Widget build(BuildContext context) {
    final String scheduleText = report.alwaysAvailable
        ? '상시 배출 가능'
        : '${_dayLabel(report.dayOfWeek)} · '
              '${_timeRange(report.startTime, report.endTime, false)}';

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              report.wasteItemName,
              style: Theme.of(context).textTheme.titleMedium,
            ),
            const SizedBox(height: 6),
            _ReportTrustBadge(report: report),
            const SizedBox(height: 6),
            Text(scheduleText),
            if (_hasText(report.note)) ...[
              const SizedBox(height: 6),
              Text(report.note!, style: Theme.of(context).textTheme.bodyMedium),
            ],
            const SizedBox(height: 10),
            Text(
              '${report.reporterNickname} 제보 · 맞아요 ${report.confirmedCount} · 달라요 ${report.differentCount}',
              style: Theme.of(context).textTheme.bodySmall,
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                Expanded(
                  child: OutlinedButton.icon(
                    onPressed: report.canConfirm
                        ? () => onConfirm('DIFFERENT')
                        : null,
                    icon: const Icon(Icons.close_rounded),
                    label: Text(
                      report.myConfirmationValue == 'DIFFERENT'
                          ? '달라요 선택됨'
                          : '정보가 달라요',
                    ),
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: ElevatedButton.icon(
                    onPressed: report.canConfirm
                        ? () => onConfirm('CONFIRMED')
                        : null,
                    icon: const Icon(Icons.check_rounded),
                    label: Text(
                      report.myConfirmationValue == 'CONFIRMED'
                          ? '맞아요 선택됨'
                          : '맞아요',
                    ),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _LocationCard extends StatelessWidget {
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
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: AppTheme.primaryColor.withValues(alpha: 0.08),
        borderRadius: BorderRadius.circular(20),
      ),
      child: Row(
        children: [
          Container(
            width: 48,
            height: 48,
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(15),
            ),
            child: const Icon(
              Icons.location_on_outlined,
              color: AppTheme.primaryColor,
            ),
          ),

          const SizedBox(width: 14),

          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style: const TextStyle(
                    color: AppTheme.primaryColor,
                    fontWeight: FontWeight.w700,
                    fontSize: 13,
                  ),
                ),

                const SizedBox(height: 4),

                Text(address, style: Theme.of(context).textTheme.titleMedium),

                const SizedBox(height: 3),

                Text(subtitle, style: Theme.of(context).textTheme.bodyMedium),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _GeneralScheduleCard extends StatelessWidget {
  const _GeneralScheduleCard({required this.schedule});

  final GeneralHousingScheduleItem schedule;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(18),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Container(
                  width: 44,
                  height: 44,
                  decoration: BoxDecoration(
                    color: AppTheme.primaryColor.withValues(alpha: 0.09),
                    borderRadius: BorderRadius.circular(14),
                  ),
                  child: Icon(
                    _iconForWasteType(schedule.wasteType),
                    color: AppTheme.primaryColor,
                  ),
                ),

                const SizedBox(width: 12),

                Expanded(
                  child: Text(
                    schedule.wasteTypeLabel,
                    style: Theme.of(context).textTheme.titleMedium,
                  ),
                ),

                _StatusBadge(
                  availableNow: schedule.availableNow,
                  availableToday: schedule.availableToday,
                ),
              ],
            ),

            const SizedBox(height: 16),

            if (!schedule.collectionAreaMatched)
              const _InlineNotice(
                message:
                    '현재 주소에 맞는 수거구역이 '
                    '아직 연결되지 않았습니다.',
              )
            else if (!schedule.scheduleAvailable &&
                schedule.todayException == null)
              const _InlineNotice(
                message:
                    '수거구역은 연결됐지만 '
                    '등록된 공식 일정이 없습니다.',
              )
            else ...[
              if (_hasText(schedule.collectionAreaName))
                _ScheduleInfoRow(
                  label: '수거구역',
                  value: schedule.collectionAreaName!,
                ),

              if (_hasText(schedule.targetAreaName))
                _ScheduleInfoRow(
                  label: '적용지역',
                  value: schedule.targetAreaName!,
                ),

              if (_hasText(schedule.emissionDays))
                _ScheduleInfoRow(label: '배출요일', value: schedule.emissionDays!),

              if (schedule.startTime != null || schedule.endTime != null)
                _ScheduleInfoRow(
                  label: '배출시간',
                  value: _timeRange(
                    schedule.startTime,
                    schedule.endTime,
                    schedule.overnight,
                  ),
                ),

              if (_hasText(schedule.emissionPlace))
                _ScheduleInfoRow(label: '배출장소', value: schedule.emissionPlace!),

              if (_hasText(schedule.emissionMethod))
                _ScheduleInfoRow(
                  label: '배출방법',
                  value: schedule.emissionMethod!,
                ),

              if (_hasText(schedule.uncollectedDay))
                _ScheduleInfoRow(
                  label: '미수거일',
                  value: schedule.uncollectedDay!,
                ),

              if (schedule.todayException != null) ...[
                const SizedBox(height: 14),

                _ExceptionCard(exception: schedule.todayException!),
              ],

              if (!schedule.availableToday &&
                  schedule.nextAvailableDate != null) ...[
                const SizedBox(height: 14),

                Text(
                  '다음 배출일 · '
                  '${_formatDate(schedule.nextAvailableDate!)}',
                  style: const TextStyle(
                    color: AppTheme.primaryColor,
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ],
            ],
          ],
        ),
      ),
    );
  }

  IconData _iconForWasteType(String value) {
    return switch (value) {
      'LIFE_WASTE' => Icons.delete_outline_rounded,
      'FOOD_WASTE' => Icons.restaurant_outlined,
      'RECYCLABLE' => Icons.recycling_rounded,
      _ => Icons.delete_outline_rounded,
    };
  }
}

class _ManagedScheduleCard extends StatelessWidget {
  const _ManagedScheduleCard({required this.item, required this.onCorrection});

  final ManagedScheduleItem item;
  final VoidCallback onCorrection;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(18),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Container(
                  width: 44,
                  height: 44,
                  decoration: BoxDecoration(
                    color: AppTheme.primaryColor.withValues(alpha: 0.09),
                    borderRadius: BorderRadius.circular(14),
                  ),
                  child: const Icon(
                    Icons.recycling_rounded,
                    color: AppTheme.primaryColor,
                  ),
                ),

                const SizedBox(width: 12),

                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        item.name,
                        style: Theme.of(context).textTheme.titleMedium,
                      ),

                      if (item.categoryName.isNotEmpty)
                        Text(
                          item.categoryName,
                          style: Theme.of(context).textTheme.bodyMedium,
                        ),
                    ],
                  ),
                ),

                _StatusBadge(
                  availableNow: item.availableNow,
                  availableToday: item.availableToday,
                ),
              ],
            ),

            if (item.schedules.isNotEmpty) ...[
              const SizedBox(height: 16),

              ...item.schedules.map((schedule) {
                return Padding(
                  padding: const EdgeInsets.only(bottom: 7),
                  child: Row(
                    children: [
                      SizedBox(
                        width: 48,
                        child: Text(
                          schedule.alwaysAvailable
                              ? '상시'
                              : _dayLabel(schedule.dayOfWeek),
                          style: const TextStyle(fontWeight: FontWeight.w700),
                        ),
                      ),

                      Expanded(
                        child: Text(
                          schedule.alwaysAvailable
                              ? '언제든 배출 가능'
                              : _timeRange(
                                  schedule.startTime,
                                  schedule.endTime,
                                  false,
                                ),
                        ),
                      ),
                    ],
                  ),
                );
              }),
            ],

            if (item.todayException != null) ...[
              const SizedBox(height: 12),

              _ExceptionCard(exception: item.todayException!),
            ],

            if (!item.availableToday && item.nextAvailableDate != null) ...[
              const SizedBox(height: 10),

              Text(
                '다음 배출일 · '
                '${_formatDate(item.nextAvailableDate!)}',
                style: const TextStyle(
                  color: AppTheme.primaryColor,
                  fontWeight: FontWeight.w700,
                ),
              ),
            ],
            const SizedBox(height: 12),
            Align(
              alignment: Alignment.centerRight,
              child: TextButton.icon(
                onPressed: onCorrection,
                icon: const Icon(Icons.report_outlined),
                label: const Text('이 일정이 달라요'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _StatusBadge extends StatelessWidget {
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
      padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 5),
      decoration: BoxDecoration(
        color: AppTheme.primaryColor.withValues(
          alpha: availableNow ? 0.14 : 0.07,
        ),
        borderRadius: BorderRadius.circular(999),
      ),
      child: Text(
        label,
        style: TextStyle(
          color: AppTheme.primaryColor,
          fontWeight: FontWeight.w700,
          fontSize: 11,
        ),
      ),
    );
  }
}

class _ExceptionCard extends StatelessWidget {
  const _ExceptionCard({required this.exception});

  final ScheduleExceptionInfo exception;

  @override
  Widget build(BuildContext context) {
    String message;

    if (exception.unavailable) {
      message = '오늘은 배출할 수 없습니다.';
    } else if (exception.alwaysAvailable == true) {
      message = '오늘은 상시 배출 가능합니다.';
    } else {
      message = '오늘은 예외 일정이 적용됩니다.';

      if (exception.startTime != null || exception.endTime != null) {
        message +=
            ' ${_timeRange(exception.startTime, exception.endTime, false)}';
      }
    }

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(13),
      decoration: BoxDecoration(
        color: const Color(0xFFFFF7E8),
        borderRadius: BorderRadius.circular(14),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Icon(Icons.campaign_outlined, size: 19),

              const SizedBox(width: 8),

              Expanded(
                child: Text(
                  message,
                  style: const TextStyle(fontWeight: FontWeight.w700),
                ),
              ),
            ],
          ),

          if (_hasText(exception.reason)) ...[
            const SizedBox(height: 6),

            Text(exception.reason!),
          ],
        ],
      ),
    );
  }
}

class _ScheduleInfoRow extends StatelessWidget {
  const _ScheduleInfoRow({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(top: 9),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 70,
            child: Text(
              label,
              style: const TextStyle(
                color: AppTheme.textSecondaryColor,
                fontSize: 13,
              ),
            ),
          ),

          Expanded(child: Text(value)),
        ],
      ),
    );
  }
}

class _InlineNotice extends StatelessWidget {
  const _InlineNotice({required this.message});

  final String message;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: const Color(0xFFF6F8F7),
        borderRadius: BorderRadius.circular(14),
      ),
      child: Text(message, style: Theme.of(context).textTheme.bodyMedium),
    );
  }
}

class _EmptyCard extends StatelessWidget {
  const _EmptyCard({required this.message});

  final String message;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(22),
        child: Row(
          children: [
            const Icon(
              Icons.calendar_month_outlined,
              color: AppTheme.primaryColor,
            ),

            const SizedBox(width: 14),

            Expanded(child: Text(message)),
          ],
        ),
      ),
    );
  }
}

String _dayLabel(String? value) {
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

String _timeRange(String? start, String? end, bool overnight) {
  final String startText = _formatTime(start);

  final String endText = _formatTime(end);

  String value = '$startText ~ $endText';

  if (overnight) {
    value += ' (익일)';
  }

  return value;
}

String _formatTime(String? value) {
  if (value == null || value.isEmpty) {
    return '-';
  }

  final List<String> parts = value.split(':');

  if (parts.length < 2) {
    return value;
  }

  return '${parts[0]}:${parts[1]}';
}

String _formatDate(String value) {
  final DateTime? date = DateTime.tryParse(value);

  if (date == null) {
    return value;
  }

  return '${date.month}월 '
      '${date.day}일';
}

bool _hasText(String? value) {
  return value != null && value.trim().isNotEmpty;
}
