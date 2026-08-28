import 'package:flutter/material.dart';
import 'package:smart_recycle/app/app_routes.dart';

class ResidenceSetupPage extends StatefulWidget {
  const ResidenceSetupPage({super.key});

  @override
  State<ResidenceSetupPage> createState() {
    return _ResidenceSetupPageState();
  }
}

class _ResidenceSetupPageState extends State<ResidenceSetupPage> {
  String? _selectedResidenceType;

  final List<String> _residenceTypes = [
    '아파트',
    '오피스텔',
    '단독주택',
    '다가구주택',
    '연립주택',
    '다세대주택',
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('거주지 설정'),
      ),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(20),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text(
                '어디에 거주하고 계신가요?',
                style: Theme.of(context).textTheme.headlineSmall,
              ),

              const SizedBox(height: 8),

              Text(
                '주소와 주거 형태를 기준으로 '
                    '맞춤형 배출 일정을 알려드려요.',
                style: Theme.of(context).textTheme.bodyMedium,
              ),

              const SizedBox(height: 28),

              TextField(
                readOnly: true,
                onTap: () {
                  // 다음 단계에서 주소 검색 API를 연결합니다.
                },
                decoration: const InputDecoration(
                  labelText: '주소',
                  hintText: '도로명 또는 지번 주소 검색',
                  prefixIcon: Icon(
                    Icons.location_on_outlined,
                  ),
                  suffixIcon: Icon(
                    Icons.search_rounded,
                  ),
                ),
              ),

              const SizedBox(height: 24),

              Text(
                '주거 형태',
                style: Theme.of(context).textTheme.titleMedium,
              ),

              const SizedBox(height: 12),

              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: _residenceTypes.map(
                      (type) {
                    final isSelected =
                        _selectedResidenceType == type;

                    return ChoiceChip(
                      label: Text(type),
                      selected: isSelected,
                      onSelected: (_) {
                        setState(() {
                          _selectedResidenceType = type;
                        });
                      },
                    );
                  },
                ).toList(),
              ),

              const Spacer(),

              ElevatedButton(
                onPressed: _selectedResidenceType == null
                    ? null
                    : () {
                  Navigator.pushNamedAndRemoveUntil(
                    context,
                    AppRoutes.home,
                        (route) => false,
                  );
                },
                child: const Text('설정 완료'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}