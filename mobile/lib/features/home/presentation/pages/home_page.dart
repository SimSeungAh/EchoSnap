import 'package:flutter/material.dart';
import 'package:smart_recycle/core/theme/app_theme.dart';

class HomePage extends StatelessWidget {
  const HomePage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(20),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const SizedBox(height: 12),

              const Text(
                'SmartRecycle',
                style: TextStyle(
                  fontSize: 28,
                  fontWeight: FontWeight.w800,
                  color: AppTheme.primaryColor,
                ),
              ),

              const SizedBox(height: 6),

              Text(
                '오늘도 헷갈리지 않게, 올바르게 분리배출해요.',
                style: Theme.of(context).textTheme.bodyMedium,
              ),

              const SizedBox(height: 28),

              TextField(
                readOnly: true,
                decoration: InputDecoration(
                  hintText: '버릴 물건을 검색해보세요',
                  prefixIcon: const Icon(
                    Icons.search_rounded,
                  ),
                  suffixIcon: IconButton(
                    onPressed: () {},
                    icon: const Icon(
                      Icons.camera_alt_outlined,
                    ),
                  ),
                ),
              ),

              const SizedBox(height: 28),

              Text(
                '오늘의 배출 일정',
                style: Theme.of(context).textTheme.titleLarge,
              ),

              const SizedBox(height: 14),

              Card(
                child: Padding(
                  padding: const EdgeInsets.all(20),
                  child: Row(
                    children: [
                      Container(
                        width: 52,
                        height: 52,
                        decoration: BoxDecoration(
                          color: AppTheme.primaryColor.withValues(
                            alpha: 0.1,
                          ),
                          borderRadius: BorderRadius.circular(16),
                        ),
                        child: const Icon(
                          Icons.recycling_rounded,
                          color: AppTheme.primaryColor,
                          size: 28,
                        ),
                      ),

                      const SizedBox(width: 16),

                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              '거주지를 설정해주세요',
                              style: Theme.of(context)
                                  .textTheme
                                  .titleMedium,
                            ),

                            const SizedBox(height: 4),

                            Text(
                              '주소를 설정하면 오늘 배출 가능한 품목을 '
                                  '알려드려요.',
                              style: Theme.of(context)
                                  .textTheme
                                  .bodyMedium,
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
              ),

              const SizedBox(height: 28),

              Text(
                '빠른 메뉴',
                style: Theme.of(context).textTheme.titleLarge,
              ),

              const SizedBox(height: 14),

              Row(
                children: [
                  Expanded(
                    child: _QuickMenuCard(
                      icon: Icons.camera_alt_outlined,
                      title: 'AI 촬영',
                      description: '사진으로 품목 확인',
                      onTap: () {},
                    ),
                  ),

                  const SizedBox(width: 12),

                  Expanded(
                    child: _QuickMenuCard(
                      icon: Icons.search_rounded,
                      title: '품목 검색',
                      description: '이름으로 찾아보기',
                      onTap: () {},
                    ),
                  ),
                ],
              ),

              const SizedBox(height: 12),

              Row(
                children: [
                  Expanded(
                    child: _QuickMenuCard(
                      icon: Icons.calendar_month_outlined,
                      title: '배출 일정',
                      description: '지역 일정 확인',
                      onTap: () {},
                    ),
                  ),

                  const SizedBox(width: 12),

                  Expanded(
                    child: _QuickMenuCard(
                      icon: Icons.home_outlined,
                      title: '거주지 설정',
                      description: '주소와 주거형태',
                      onTap: () {},
                    ),
                  ),
                ],
              ),

              const SizedBox(height: 32),
            ],
          ),
        ),
      ),
    );
  }
}

class _QuickMenuCard extends StatelessWidget {
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
        borderRadius: BorderRadius.circular(20),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                width: 42,
                height: 42,
                decoration: BoxDecoration(
                  color: AppTheme.primaryColor.withValues(
                    alpha: 0.1,
                  ),
                  borderRadius: BorderRadius.circular(13),
                ),
                child: Icon(
                  icon,
                  color: AppTheme.primaryColor,
                ),
              ),

              const SizedBox(height: 18),

              Text(
                title,
                style: Theme.of(context).textTheme.titleMedium,
              ),

              const SizedBox(height: 4),

              Text(
                description,
                style: Theme.of(context).textTheme.bodyMedium?.copyWith(
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