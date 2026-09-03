import 'package:flutter/material.dart';
import 'package:echosnap/app/app_routes.dart';
import 'package:echosnap/core/storage/token_storage.dart';
import 'package:echosnap/core/theme/app_theme.dart';
import 'package:echosnap/features/user/data/current_user_api.dart';

class SplashPage extends StatefulWidget {
  const SplashPage({
    super.key,
  });

  @override
  State<SplashPage> createState() {
    return _SplashPageState();
  }
}

class _SplashPageState extends State<SplashPage> {
  bool _hasError = false;

  String _statusMessage =
      '로그인 정보를 확인하고 있어요.';

  @override
  void initState() {
    super.initState();

    _checkSession();
  }

  Future<void> _checkSession() async {
    if (!mounted) {
      return;
    }

    setState(() {
      _hasError = false;
      _statusMessage =
      '로그인 정보를 확인하고 있어요.';
    });

    final String? accessToken =
    await TokenStorage.getAccessToken();

    if (!mounted) {
      return;
    }

    /*
     * Access Token 자체가 없다면
     * 서버 호출 없이 바로 로그인 화면으로 이동합니다.
     */
    if (accessToken == null ||
        accessToken.isEmpty) {
      _moveToLogin();
      return;
    }

    try {
      /*
       * 실제 인증 유효성은 서버에서 확인합니다.
       *
       * Access Token이 만료되어 401이 발생하면
       * AuthenticatedApiClient가 내부에서
       * Refresh Token 재발급과 원 요청 재시도를 처리합니다.
       */
      await CurrentUserApi.getMe();

      if (!mounted) {
        return;
      }

      _moveToHome();
    } on CurrentUserApiException catch (exception) {
      if (!mounted) {
        return;
      }

      if (exception.unauthorized) {
        await TokenStorage.clearTokens();

        if (!mounted) {
          return;
        }

        _moveToLogin();
        return;
      }

      /*
       * 서버 장애나 네트워크 문제라면
       * 유효할 수도 있는 토큰을 삭제하지 않습니다.
       */
      setState(() {
        _hasError = true;
        _statusMessage =
            exception.message;
      });
    } catch (_) {
      if (!mounted) {
        return;
      }

      setState(() {
        _hasError = true;
        _statusMessage =
        '앱 시작 중 오류가 발생했습니다.';
      });
    }
  }

  void _moveToLogin() {
    Navigator.pushNamedAndRemoveUntil(
      context,
      AppRoutes.login,
          (route) => false,
    );
  }

  void _moveToHome() {
    Navigator.pushNamedAndRemoveUntil(
      context,
      AppRoutes.home,
          (route) => false,
    );
  }

  @override
  Widget build(
      BuildContext context,
      ) {
    return Scaffold(
      backgroundColor:
      AppTheme.primaryColor,
      body: SafeArea(
        child: Center(
          child: Padding(
            padding:
            const EdgeInsets.all(24),
            child: Column(
              mainAxisSize:
              MainAxisSize.min,
              children: [
                const Icon(
                  Icons.recycling_rounded,
                  size: 76,
                  color: Colors.white,
                ),

                const SizedBox(
                  height: 18,
                ),

                const Text(
                  'EchoSnap',
                  style: TextStyle(
                    color: Colors.white,
                    fontSize: 32,
                    fontWeight:
                    FontWeight.w800,
                  ),
                ),

                const SizedBox(
                  height: 8,
                ),

                const Text(
                  '헷갈리는 분리배출을 쉽게 확인해요',
                  textAlign:
                  TextAlign.center,
                  style: TextStyle(
                    color: Colors.white70,
                    fontSize: 14,
                  ),
                ),

                const SizedBox(
                  height: 34,
                ),

                if (!_hasError)
                  const SizedBox(
                    width: 26,
                    height: 26,
                    child:
                    CircularProgressIndicator(
                      strokeWidth: 2.5,
                      color: Colors.white,
                    ),
                  ),

                if (_hasError)
                  const Icon(
                    Icons
                        .error_outline_rounded,
                    color: Colors.white,
                    size: 30,
                  ),

                const SizedBox(
                  height: 14,
                ),

                Text(
                  _statusMessage,
                  textAlign:
                  TextAlign.center,
                  style: const TextStyle(
                    color: Colors.white70,
                    fontSize: 13,
                  ),
                ),

                if (_hasError) ...[
                  const SizedBox(
                    height: 18,
                  ),

                  OutlinedButton(
                    onPressed:
                    _checkSession,
                    style:
                    OutlinedButton.styleFrom(
                      foregroundColor:
                      Colors.white,
                      side:
                      const BorderSide(
                        color: Colors.white,
                      ),
                    ),
                    child:
                    const Text(
                      '다시 시도',
                    ),
                  ),
                ],
              ],
            ),
          ),
        ),
      ),
    );
  }
}
