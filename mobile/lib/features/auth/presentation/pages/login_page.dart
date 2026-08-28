import 'package:flutter/material.dart';
import 'package:smart_recycle/app/app_routes.dart';

class LoginPage extends StatelessWidget {
  const LoginPage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const Spacer(),

              Icon(
                Icons.recycling_rounded,
                size: 64,
                color: Theme.of(context).colorScheme.primary,
              ),

              const SizedBox(height: 20),

              Text(
                'SmartRecycle',
                textAlign: TextAlign.center,
                style: Theme.of(context).textTheme.headlineSmall,
              ),

              const SizedBox(height: 8),

              Text(
                '로그인하고 내 거주지에 맞는\n'
                    '분리배출 정보를 확인해보세요.',
                textAlign: TextAlign.center,
                style: Theme.of(context).textTheme.bodyMedium,
              ),

              const SizedBox(height: 40),

              const TextField(
                keyboardType: TextInputType.emailAddress,
                decoration: InputDecoration(
                  labelText: '이메일',
                  hintText: 'example@email.com',
                  prefixIcon: Icon(
                    Icons.email_outlined,
                  ),
                ),
              ),

              const SizedBox(height: 14),

              const TextField(
                obscureText: true,
                decoration: InputDecoration(
                  labelText: '비밀번호',
                  prefixIcon: Icon(
                    Icons.lock_outline_rounded,
                  ),
                ),
              ),

              const SizedBox(height: 24),

              ElevatedButton(
                onPressed: () {
                  Navigator.pushReplacementNamed(
                    context,
                    AppRoutes.residenceSetup,
                  );
                },
                child: const Text('로그인'),
              ),

              const SizedBox(height: 12),

              TextButton(
                onPressed: () {},
                child: const Text(
                  '처음이신가요? 회원가입',
                ),
              ),

              const Spacer(),
            ],
          ),
        ),
      ),
    );
  }
}