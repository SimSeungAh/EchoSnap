import 'package:flutter/material.dart';
import 'package:echosnap/app/app_routes.dart';
import 'package:echosnap/core/storage/token_storage.dart';
import 'package:echosnap/core/theme/app_theme.dart';
import 'package:echosnap/features/auth/data/auth_api.dart';
import 'package:echosnap/features/home/data/home_schedule_api.dart';
import 'package:echosnap/features/user/data/current_user_api.dart';

class HomePage extends StatefulWidget {
  const HomePage({
    super.key,
  });

  @override
  State<HomePage> createState() =>
      _HomePageState();
}

class _HomePageState
    extends State<HomePage> {
  CurrentUser? _user;
  HomeScheduleSnapshot? _schedule;

  bool _isLoading = true;
  bool _isLoggingOut = false;

  String? _scheduleError;

  @override
  void initState() {
    super.initState();

    _loadHome();
  }

  Future<void> _loadHome() async {
    if (mounted) {
      setState(() {
        _isLoading = true;
        _scheduleError = null;
      });
    }

    CurrentUser user;

    try {
      user =
      await CurrentUserApi.getMe();
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
        _isLoading = false;
        _scheduleError =
            exception.message;
      });

      return;
    }

    HomeScheduleSnapshot? schedule;
    String? scheduleError;

    final String? residenceType =
        user.residenceType;

    if (residenceType != null) {
      try {
        schedule =
        await HomeScheduleApi
            .getSchedule(
          residenceType:
          residenceType,
        );
      } on HomeScheduleApiException catch (
      exception
      ) {
        if (!mounted) {
          return;
        }

        if (exception.unauthorized) {
          await _moveToLogin();
          return;
        }

        scheduleError =
            exception.message;
      }
    }

    if (!mounted) {
      return;
    }

    setState(() {
      _user = user;
      _schedule = schedule;
      _scheduleError =
          scheduleError;
      _isLoading = false;
    });
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

  Future<void> _confirmLogout() async {
    if (_isLoggingOut) {
      return;
    }

    final bool? confirmed = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('로그아웃할까요?'),
        content: const Text('다시 이용하려면 로그인이 필요해요.'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(dialogContext, false),
            child: const Text('취소'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(dialogContext, true),
            child: const Text('로그아웃'),
          ),
        ],
      ),
    );

    if (confirmed == true && mounted) {
      await _logout();
    }
  }

  Future<void> _logout() async {
    setState(() {
      _isLoggingOut = true;
    });

    final String? refreshToken =
        await TokenStorage.getRefreshToken();

    if (refreshToken != null && refreshToken.isNotEmpty) {
      try {
        await AuthApi.logout(
          refreshToken: refreshToken,
        );
      } on AuthApiException {
        // 서버가 응답하지 않아도 기기의 로그인 정보는 삭제합니다.
      }
    }

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
      body: SafeArea(
        child: RefreshIndicator(
          onRefresh: _loadHome,
          child: SingleChildScrollView(
            physics:
            const AlwaysScrollableScrollPhysics(),
            padding:
            const EdgeInsets.all(20),
            child: Column(
              crossAxisAlignment:
              CrossAxisAlignment.start,
              children: [
                const SizedBox(height: 12),

                Row(
                  children: [
                    const Expanded(
                      child: Text(
                        'EchoSnap',
                        style: TextStyle(
                          fontSize: 28,
                          fontWeight:
                          FontWeight.w800,
                          color:
                          AppTheme.primaryColor,
                        ),
                      ),
                    ),
                    IconButton(
                      tooltip: '로그아웃',
                      onPressed: _isLoggingOut
                          ? null
                          : _confirmLogout,
                      icon: _isLoggingOut
                          ? const SizedBox(
                              width: 22,
                              height: 22,
                              child: CircularProgressIndicator(
                                strokeWidth: 2,
                              ),
                            )
                          : const Icon(
                              Icons.logout_rounded,
                            ),
                    ),
                  ],
                ),

                const SizedBox(height: 6),

                Text(
                  _user?.nickname
                      .trim()
                      .isNotEmpty ==
                      true
                      ? '${_user!.nickname}님, '
                      '오늘도 올바르게 분리배출해요.'
                      : '오늘도 헷갈리지 않게, '
                      '올바르게 분리배출해요.',
                  style: Theme.of(context)
                      .textTheme
                      .bodyMedium,
                ),

                const SizedBox(height: 12),

                if (_user != null)
                  _ResidenceSummary(
                    user: _user!,
                  ),

                const SizedBox(height: 24),

                TextField(
                  readOnly: true,
                  onTap: () {
                    Navigator.pushNamed(
                      context,
                      AppRoutes.wasteSearch,
                    );
                  },
                  decoration: InputDecoration(
                    hintText:
                    '버릴 물건을 검색해보세요',
                    prefixIcon:
                    const Icon(
                      Icons.search_rounded,
                    ),
                    suffixIcon:
                    IconButton(
                      tooltip: 'AI 촬영',
                      onPressed: () {
                        Navigator.pushNamed(
                          context,
                          AppRoutes.aiCapture,
                        );
                      },
                      icon: const Icon(
                        Icons
                            .camera_alt_outlined,
                      ),
                    ),
                  ),
                ),

                const SizedBox(height: 28),

                Row(
                  children: [
                    Expanded(
                      child: Text(
                        '오늘의 배출 일정',
                        style:
                        Theme.of(context)
                            .textTheme
                            .titleLarge,
                      ),
                    ),

                    IconButton(
                      tooltip: '새로고침',
                      onPressed:
                      _isLoading
                          ? null
                          : _loadHome,
                      icon: const Icon(
                        Icons.refresh_rounded,
                      ),
                    ),
                  ],
                ),

                const SizedBox(height: 10),

                _buildScheduleCard(),

                const SizedBox(height: 28),

                Text(
                  '빠른 메뉴',
                  style: Theme.of(context)
                      .textTheme
                      .titleLarge,
                ),

                const SizedBox(height: 14),

                Row(
                  children: [
                    Expanded(
                      child: _QuickMenuCard(
                        icon: Icons
                            .camera_alt_outlined,
                        title: 'AI 촬영',
                        description:
                        '사진으로 품목 확인',
                        onTap: () {
                          Navigator.pushNamed(
                            context,
                            AppRoutes.aiCapture,
                          );
                        },
                      ),
                    ),

                    const SizedBox(width: 12),

                    Expanded(
                      child: _QuickMenuCard(
                        icon:
                        Icons.search_rounded,
                        title: '품목 검색',
                        description:
                        '이름으로 찾아보기',
                        onTap: () {
                          Navigator.pushNamed(
                            context,
                            AppRoutes
                                .wasteSearch,
                          );
                        },
                      ),
                    ),
                  ],
                ),

                const SizedBox(height: 12),

                Row(
                  children: [
                    Expanded(
                      child: _QuickMenuCard(
                        icon: Icons
                            .calendar_month_outlined,
                        title: '배출 일정',
                        description:
                        '지역 일정 확인',
                        onTap: () {
                          Navigator.pushNamed(
                            context,
                            AppRoutes.schedule,
                          );
                        },
                      ),
                    ),

                    const SizedBox(width: 12),

                    Expanded(
                      child: _QuickMenuCard(
                        icon:
                        Icons.home_outlined,
                        title: '거주지 설정',
                        description:
                        '배출 장소와 주소',
                        onTap: () async {
                          await Navigator
                              .pushNamed(
                            context,
                            AppRoutes
                                .residenceSetup,
                          );

                          if (mounted) {
                            await _loadHome();
                          }
                        },
                      ),
                    ),
                  ],
                ),

                const SizedBox(height: 32),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildScheduleCard() {
    if (_isLoading) {
      return const Card(
        child: Padding(
          padding: EdgeInsets.all(24),
          child: Center(
            child:
            CircularProgressIndicator(),
          ),
        ),
      );
    }

    final CurrentUser? user = _user;

    if (user == null ||
        user.residenceType == null) {
      return const _ScheduleCard(
        icon: Icons.home_outlined,
        title: '거주지를 설정해주세요',
        description:
        '주소를 설정하면 오늘 배출 가능한 '
            '품목을 알려드려요.',
      );
    }

    if (_scheduleError != null) {
      return _ScheduleCard(
        icon:
        Icons.error_outline_rounded,
        title:
        '배출 일정을 불러오지 못했어요',
        description: _scheduleError!,
      );
    }

    final HomeScheduleSnapshot? schedule =
        _schedule;

    if (schedule == null ||
        schedule.entries.isEmpty) {
      return const _ScheduleCard(
        icon:
        Icons.calendar_month_outlined,
        title:
        '등록된 배출 일정이 없어요',
        description:
        '현재 거주지에 연결된 공식 일정이 '
            '아직 없습니다.',
      );
    }

    final nowEntries =
        schedule.availableNowEntries;

    if (nowEntries.isNotEmpty) {
      return _ScheduleCard(
        icon:
        Icons.check_circle_rounded,
        title: '지금 배출할 수 있어요',
        description:
        _joinLabels(nowEntries),
      );
    }

    final todayEntries =
        schedule.availableTodayEntries;

    if (todayEntries.isNotEmpty) {
      return _ScheduleCard(
        icon: Icons.schedule_rounded,
        title:
        '오늘 배출 일정이 있어요',
        description:
        _joinLabels(todayEntries),
      );
    }

    final HomeScheduleEntry? next =
        schedule.nearestNextEntry;

    if (next != null) {
      return _ScheduleCard(
        icon:
        Icons.event_outlined,
        title: '오늘은 배출 일정이 없어요',
        description:
        '${next.label} · 다음 배출일 '
            '${_formatDate(next.nextAvailableDate)}',
      );
    }

    return const _ScheduleCard(
      icon:
      Icons.calendar_today_outlined,
      title:
      '오늘은 배출 일정이 없어요',
      description:
      '현재 확인 가능한 다음 일정도 없습니다.',
    );
  }

  String _joinLabels(
      List<HomeScheduleEntry> entries,
      ) {
    return entries
        .map(
          (entry) => entry.label,
    )
        .join(' · ');
  }

  String _formatDate(
      String? value,
      ) {
    if (value == null ||
        value.isEmpty) {
      return '미정';
    }

    final DateTime? date =
    DateTime.tryParse(value);

    if (date == null) {
      return value;
    }

    return '${date.month}월 '
        '${date.day}일';
  }
}

class _ResidenceSummary
    extends StatelessWidget {
  const _ResidenceSummary({
    required this.user,
  });

  final CurrentUser user;

  @override
  Widget build(BuildContext context) {
    final String title;
    final String description;
    final IconData icon;

    if (user.residenceType ==
        'GENERAL_HOUSING') {
      title = '일반주택';

      description =
          user.residence?.displayAddress ??
              '주소 정보 없음';

      icon = Icons.home_outlined;
    } else if (user.residenceType ==
        'MANAGED_COMPLEX') {
      title =
          user.apartment?.name ??
              '공동주택';

      description =
          user.apartment?.roadAddress ??
              user.apartment?.jibunAddress ??
              '주소 정보 없음';

      icon =
          Icons.apartment_rounded;
    } else {
      title = '거주지 미설정';
      description =
      '거주지 정보를 설정해주세요.';
      icon = Icons.home_outlined;
    }

    return Container(
      width: double.infinity,
      padding:
      const EdgeInsets.symmetric(
        horizontal: 14,
        vertical: 12,
      ),
      decoration: BoxDecoration(
        color: AppTheme.primaryColor
            .withValues(
          alpha: 0.07,
        ),
        borderRadius:
        BorderRadius.circular(16),
      ),
      child: Row(
        children: [
          Icon(
            icon,
            size: 21,
            color:
            AppTheme.primaryColor,
          ),

          const SizedBox(width: 10),

          Expanded(
            child: Column(
              crossAxisAlignment:
              CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style:
                  const TextStyle(
                    color: AppTheme
                        .primaryColor,
                    fontWeight:
                    FontWeight.w700,
                    fontSize: 13,
                  ),
                ),

                const SizedBox(height: 2),

                Text(
                  description,
                  overflow:
                  TextOverflow.ellipsis,
                  style: const TextStyle(
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

class _ScheduleCard
    extends StatelessWidget {
  const _ScheduleCard({
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
        const EdgeInsets.all(20),
        child: Row(
          children: [
            Container(
              width: 52,
              height: 52,
              decoration:
              BoxDecoration(
                color: AppTheme.primaryColor
                    .withValues(
                  alpha: 0.1,
                ),
                borderRadius:
                BorderRadius.circular(
                  16,
                ),
              ),
              child: Icon(
                icon,
                color:
                AppTheme.primaryColor,
                size: 28,
              ),
            ),

            const SizedBox(width: 16),

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

                  const SizedBox(height: 5),

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

class _QuickMenuCard
    extends StatelessWidget {
  const _QuickMenuCard({
    required this.icon,
    required this.title,
    required this.description,
    required this.onTap,
  });

  final IconData icon;
  final String title;
  final String description;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: InkWell(
        onTap: onTap,
        borderRadius:
        BorderRadius.circular(20),
        child: Padding(
          padding:
          const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment:
            CrossAxisAlignment.start,
            children: [
              Container(
                width: 42,
                height: 42,
                decoration:
                BoxDecoration(
                  color: AppTheme
                      .primaryColor
                      .withValues(
                    alpha: 0.1,
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

              const SizedBox(height: 18),

              Text(
                title,
                style: Theme.of(context)
                    .textTheme
                    .titleMedium,
              ),

              const SizedBox(height: 4),

              Text(
                description,
                style: Theme.of(context)
                    .textTheme
                    .bodyMedium
                    ?.copyWith(
                  fontSize: 12,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
