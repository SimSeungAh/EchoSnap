import 'package:flutter/material.dart';
import 'package:echosnap/app/app_routes.dart';
import 'package:echosnap/core/theme/app_theme.dart';

class EchoSnapApp extends StatelessWidget {
  const EchoSnapApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'EchoSnap',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.light,
      initialRoute: AppRoutes.splash,
      onGenerateRoute: AppRoutes.onGenerateRoute,
    );
  }
}
