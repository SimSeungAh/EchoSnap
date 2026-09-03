import 'dart:convert';

import 'package:http/http.dart' as http;
import 'package:echosnap/core/config/app_config.dart';

class AuthToken {
  const AuthToken({
    required this.accessToken,
    required this.refreshToken,
  });

  final String accessToken;
  final String refreshToken;

  factory AuthToken.fromJson(
      Map<String, dynamic> json,
      ) {
    return AuthToken(
      accessToken:
      json['accessToken'] as String? ?? '',
      refreshToken:
      json['refreshToken'] as String? ?? '',
    );
  }
}

class AuthApiException implements Exception {
  const AuthApiException(
      this.message, {
        this.statusCode,
      });

  final String message;
  final int? statusCode;

  bool get unauthorized =>
      statusCode == 401 ||
          statusCode == 403;

  @override
  String toString() {
    return message;
  }
}

class AuthApi {
  AuthApi._();

  static Future<AuthToken> login({
    required String email,
    required String password,
  }) async {
    final Uri uri = Uri.parse(
      '${AppConfig.apiBaseUrl}/api/auth/login',
    );

    return _requestToken(
      uri: uri,
      body: {
        'email': email,
        'password': password,
      },
      defaultErrorMessage:
      '로그인 처리 중 오류가 발생했습니다.',
    );
  }

  static Future<AuthToken> reissue({
    required String refreshToken,
  }) async {
    final Uri uri = Uri.parse(
      '${AppConfig.apiBaseUrl}/api/auth/reissue',
    );

    return _requestToken(
      uri: uri,
      body: {
        'refreshToken': refreshToken,
      },
      defaultErrorMessage:
      '로그인 정보를 갱신하지 못했습니다.',
    );
  }

  static Future<AuthToken> _requestToken({
    required Uri uri,
    required Map<String, dynamic> body,
    required String defaultErrorMessage,
  }) async {
    late http.Response response;

    try {
      response = await http
          .post(
        uri,
        headers: const {
          'Content-Type':
          'application/json; charset=UTF-8',
          'Accept': 'application/json',
        },
        body: jsonEncode(body),
      )
          .timeout(
        const Duration(seconds: 10),
      );
    } catch (_) {
      throw const AuthApiException(
        '서버에 연결할 수 없습니다. '
            '백엔드 실행 상태를 확인해주세요.',
      );
    }

    Map<String, dynamic> responseBody;

    try {
      responseBody = jsonDecode(
        utf8.decode(response.bodyBytes),
      ) as Map<String, dynamic>;
    } catch (_) {
      throw AuthApiException(
        '서버 응답을 처리할 수 없습니다. '
            '(HTTP ${response.statusCode})',
        statusCode: response.statusCode,
      );
    }

    final String message =
        responseBody['message'] as String? ??
            defaultErrorMessage;

    if (response.statusCode < 200 ||
        response.statusCode >= 300) {
      throw AuthApiException(
        message,
        statusCode: response.statusCode,
      );
    }

    if (responseBody['success'] != true) {
      throw AuthApiException(
        message,
        statusCode: response.statusCode,
      );
    }

    final dynamic data =
    responseBody['data'];

    if (data is! Map<String, dynamic>) {
      throw const AuthApiException(
        '서버 응답에 토큰 정보가 없습니다.',
      );
    }

    final AuthToken token =
    AuthToken.fromJson(data);

    if (token.accessToken.isEmpty ||
        token.refreshToken.isEmpty) {
      throw const AuthApiException(
        '로그인 토큰을 확인할 수 없습니다.',
      );
    }

    return token;
  }
}