import 'package:flutter/material.dart';
import 'package:smart_recycle/features/auth/presentation/pages/login_page.dart';
import 'package:smart_recycle/features/home/presentation/pages/home_page.dart';
import 'package:smart_recycle/features/residence/presentation/pages/residence_setup_page.dart';
import 'package:smart_recycle/features/splash/presentation/pages/splash_page.dart';

class AppRoutes {
  AppRoutes._();

  static const String splash = '/';
  static const String login = '/login';
  static const String residenceSetup = '/residence-setup';
  static const String home = '/home';

  static Route<dynamic> onGenerateRoute(
      RouteSettings settings,
      ) {
    switch (settings.name) {
      case splash:
        return MaterialPageRoute(
          builder: (_) => const SplashPage(),
          settings: settings,
        );

      case login:
        return MaterialPageRoute(
          builder: (_) => const LoginPage(),
          settings: settings,
        );

      case residenceSetup:
        return MaterialPageRoute(
          builder: (_) => const ResidenceSetupPage(),
          settings: settings,
        );

      case home:
        return MaterialPageRoute(
          builder: (_) => const HomePage(),
          settings: settings,
        );

      default:
        return MaterialPageRoute(
          builder: (_) => const SplashPage(),
          settings: settings,
        );
    }
  }
}