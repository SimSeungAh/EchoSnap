import 'package:flutter/material.dart';
import 'package:smart_recycle/features/ai/presentation/pages/ai_capture_page.dart';
import 'package:smart_recycle/features/auth/presentation/pages/login_page.dart';
import 'package:smart_recycle/features/home/presentation/pages/home_page.dart';
import 'package:smart_recycle/features/residence/presentation/pages/residence_setup_page.dart';
import 'package:smart_recycle/features/schedule/presentation/pages/schedule_page.dart';
import 'package:smart_recycle/features/splash/presentation/pages/splash_page.dart';
import 'package:smart_recycle/features/waste/presentation/pages/waste_detail_page.dart';
import 'package:smart_recycle/features/waste/presentation/pages/waste_search_page.dart';

class AppRoutes {
  AppRoutes._();

  static const String splash = '/';
  static const String login = '/login';

  static const String aiCapture =
      '/ai-capture';

  static const String residenceSetup =
      '/residence-setup';

  static const String home = '/home';

  static const String wasteSearch =
      '/waste-search';

  static const String wasteDetail =
      '/waste-detail';

  static const String schedule =
      '/schedule';

  static Route<dynamic> onGenerateRoute(
      RouteSettings settings,
      ) {
    switch (settings.name) {
      case splash:
        return MaterialPageRoute(
          builder: (_) =>
          const SplashPage(),
          settings: settings,
        );

      case aiCapture:
        return MaterialPageRoute(
          builder: (_) =>
          const AiCapturePage(),
          settings: settings,
        );

      case login:
        return MaterialPageRoute(
          builder: (_) =>
          const LoginPage(),
          settings: settings,
        );

      case residenceSetup:
        return MaterialPageRoute(
          builder: (_) =>
          const ResidenceSetupPage(),
          settings: settings,
        );

      case home:
        return MaterialPageRoute(
          builder: (_) =>
          const HomePage(),
          settings: settings,
        );

      case wasteSearch:
        return MaterialPageRoute(
          builder: (_) =>
          const WasteSearchPage(),
          settings: settings,
        );

      case wasteDetail:
        final int? wasteItemId =
        settings.arguments is int
            ? settings.arguments as int
            : null;

        if (wasteItemId == null) {
          return MaterialPageRoute(
            builder: (_) =>
            const WasteSearchPage(),
            settings: settings,
          );
        }

        return MaterialPageRoute(
          builder: (_) =>
              WasteDetailPage(
                wasteItemId:
                wasteItemId,
              ),
          settings: settings,
        );

      case schedule:
        return MaterialPageRoute(
          builder: (_) =>
          const SchedulePage(),
          settings: settings,
        );

      default:
        return MaterialPageRoute(
          builder: (_) =>
          const SplashPage(),
          settings: settings,
        );
    }
  }
}