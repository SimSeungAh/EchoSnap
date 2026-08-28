import 'package:flutter/material.dart';
import 'package:smart_recycle/app/app_routes.dart';
import 'package:smart_recycle/core/storage/token_storage.dart';
import 'package:smart_recycle/features/auth/data/auth_api.dart';
import 'package:smart_recycle/features/user/data/current_user_api.dart';

class LoginPage extends StatefulWidget {
  const LoginPage({
    super.key,
  });

  @override
  State<LoginPage> createState() {
    return _LoginPageState();
  }
}

class _LoginPageState extends State<LoginPage> {
  final GlobalKey<FormState> _formKey =
  GlobalKey<FormState>();

  final TextEditingController _emailController =
  TextEditingController();

  final TextEditingController _passwordController =
  TextEditingController();

  bool _isLoading = false;
  bool _obscurePassword = true;

  @override
  void dispose() {
    _emailController.dispose();
    _passwordController.dispose();

    super.dispose();
  }

  Future<void> _login() async {
    if (_isLoading) {
      return;
    }

    final bool isValid =
        _formKey.currentState?.validate() ??
            false;

    if (!isValid) {
      return;
    }

    FocusScope.of(context).unfocus();

    setState(() {
      _isLoading = true;
    });

    try {
      final AuthToken token =
      await AuthApi.login(
        email: _emailController.text.trim(),
        password: _passwordController.text,
      );

      await TokenStorage.saveTokens(
        accessToken: token.accessToken,
        refreshToken: token.refreshToken,
      );

      /*
       * AuthenticatedApiClient가 저장된 Access Token을
       * 자동으로 사용하므로 토큰을 직접 전달하지 않습니다.
       */
      final CurrentUser user =
      await CurrentUserApi.getMe();

      if (!mounted) {
        return;
      }

      if (user.onboardingCompleted) {
        _moveToHome();
      } else {
        _moveToResidenceSetup();
      }
    } on AuthApiException catch (exception) {
      if (!mounted) {
        return;
      }

      _showMessage(
        exception.message,
      );
    } on CurrentUserApiException catch (exception) {
      /*
       * 실제 인증 거부일 때만 토큰을 삭제합니다.
       * 단순 네트워크 장애라면 방금 발급된 토큰을
       * 임의로 없애지 않습니다.
       */
      if (exception.unauthorized) {
        await TokenStorage.clearTokens();
      }

      if (!mounted) {
        return;
      }

      _showMessage(
        exception.message,
      );
    } catch (_) {
      if (!mounted) {
        return;
      }

      _showMessage(
        '로그인 중 알 수 없는 오류가 발생했습니다.',
      );
    } finally {
      if (mounted) {
        setState(() {
          _isLoading = false;
        });
      }
    }
  }

  void _moveToResidenceSetup() {
    Navigator.pushNamedAndRemoveUntil(
      context,
      AppRoutes.residenceSetup,
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

  void _showMessage(
      String message,
      ) {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(
        SnackBar(
          content: Text(
            message,
          ),
        ),
      );
  }

  String? _validateEmail(
      String? value,
      ) {
    final String email =
        value?.trim() ?? '';

    if (email.isEmpty) {
      return '이메일을 입력해주세요.';
    }

    final RegExp emailPattern = RegExp(
      r'^[^@\s]+@[^@\s]+\.[^@\s]+$',
    );

    if (!emailPattern.hasMatch(email)) {
      return '올바른 이메일 형식을 입력해주세요.';
    }

    return null;
  }

  String? _validatePassword(
      String? value,
      ) {
    if (value == null ||
        value.isEmpty) {
      return '비밀번호를 입력해주세요.';
    }

    return null;
  }

  @override
  Widget build(
      BuildContext context,
      ) {
    return Scaffold(
      body: SafeArea(
        child: Form(
          key: _formKey,
          child: SingleChildScrollView(
            padding:
            const EdgeInsets.all(24),
            child: ConstrainedBox(
              constraints: BoxConstraints(
                minHeight:
                MediaQuery.sizeOf(context)
                    .height -
                    48,
              ),
              child: IntrinsicHeight(
                child: Column(
                  crossAxisAlignment:
                  CrossAxisAlignment.stretch,
                  children: [
                    const Spacer(),

                    Icon(
                      Icons.recycling_rounded,
                      size: 64,
                      color: Theme.of(context)
                          .colorScheme
                          .primary,
                    ),

                    const SizedBox(
                      height: 20,
                    ),

                    Text(
                      'SmartRecycle',
                      textAlign:
                      TextAlign.center,
                      style: Theme.of(context)
                          .textTheme
                          .headlineSmall,
                    ),

                    const SizedBox(
                      height: 8,
                    ),

                    Text(
                      '로그인하고 내 거주지에 맞는\n'
                          '분리배출 정보를 확인해보세요.',
                      textAlign:
                      TextAlign.center,
                      style: Theme.of(context)
                          .textTheme
                          .bodyMedium,
                    ),

                    const SizedBox(
                      height: 40,
                    ),

                    TextFormField(
                      controller:
                      _emailController,
                      enabled:
                      !_isLoading,
                      keyboardType:
                      TextInputType
                          .emailAddress,
                      textInputAction:
                      TextInputAction.next,
                      autofillHints:
                      const [
                        AutofillHints.email,
                      ],
                      validator:
                      _validateEmail,
                      decoration:
                      const InputDecoration(
                        labelText: '이메일',
                        hintText:
                        'example@email.com',
                        prefixIcon: Icon(
                          Icons.email_outlined,
                        ),
                      ),
                    ),

                    const SizedBox(
                      height: 14,
                    ),

                    TextFormField(
                      controller:
                      _passwordController,
                      enabled:
                      !_isLoading,
                      obscureText:
                      _obscurePassword,
                      textInputAction:
                      TextInputAction.done,
                      autofillHints:
                      const [
                        AutofillHints.password,
                      ],
                      validator:
                      _validatePassword,
                      onFieldSubmitted: (_) {
                        _login();
                      },
                      decoration:
                      InputDecoration(
                        labelText: '비밀번호',
                        prefixIcon:
                        const Icon(
                          Icons
                              .lock_outline_rounded,
                        ),
                        suffixIcon:
                        IconButton(
                          onPressed:
                          _isLoading
                              ? null
                              : () {
                            setState(
                                  () {
                                _obscurePassword =
                                !_obscurePassword;
                              },
                            );
                          },
                          icon: Icon(
                            _obscurePassword
                                ? Icons
                                .visibility_off_outlined
                                : Icons
                                .visibility_outlined,
                          ),
                        ),
                      ),
                    ),

                    const SizedBox(
                      height: 24,
                    ),

                    ElevatedButton(
                      onPressed:
                      _isLoading
                          ? null
                          : _login,
                      child:
                      _isLoading
                          ? const SizedBox(
                        width: 22,
                        height: 22,
                        child:
                        CircularProgressIndicator(
                          strokeWidth: 2,
                          color:
                          Colors.white,
                        ),
                      )
                          : const Text(
                        '로그인',
                      ),
                    ),

                    const SizedBox(
                      height: 12,
                    ),

                    TextButton(
                      onPressed:
                      _isLoading
                          ? null
                          : () {
                        // 회원가입은 후속 단계에서 연결합니다.
                      },
                      child:
                      const Text(
                        '처음이신가요? 회원가입',
                      ),
                    ),

                    const Spacer(),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}