import 'package:flutter/material.dart';
import 'package:smart_recycle/app/app_routes.dart';
import 'package:smart_recycle/core/theme/app_theme.dart';

class SmartRecycleApp extends StatelessWidget {
  const SmartRecycleApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'SmartRecycle',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.light,
      initialRoute: AppRoutes.splash,
      onGenerateRoute: AppRoutes.onGenerateRoute,
    );
  }
}