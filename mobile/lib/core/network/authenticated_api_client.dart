import 'dart:convert';

import 'package:http/http.dart' as http;
import 'package:smart_recycle/core/config/app_config.dart';
import 'package:smart_recycle/core/storage/token_storage.dart';
import 'package:smart_recycle/features/auth/data/auth_api.dart';

class AuthenticatedApiException implements Exception {
  const AuthenticatedApiException(
      this.message, {
        this.unauthorized = false,
      });

  final String message;
  final bool unauthorized;

  @override
  String toString() {
    return message;
  }
}

class AuthenticatedApiClient {
  AuthenticatedApiClient._();

  static Future<http.Response> get(
      String path, {
        Map<String, String>? queryParameters,
      }) async {
    final Uri uri = Uri.parse(
      '${AppConfig.apiBaseUrl}$path',
    ).replace(
      queryParameters: queryParameters,
    );

    return _sendWithAuthentication(
      request: (accessToken) {
        return http
            .get(
          uri,
          headers: {
            'Accept': 'application/json',
            'Authorization': 'Bearer $accessToken',
          },
        )
            .timeout(
          const Duration(seconds: 10),
        );
      },
    );
  }

  static Future<http.Response> patch(
      String path, {
        required Map<String, dynamic> body,
      }) async {
    final Uri uri = Uri.parse(
      '${AppConfig.apiBaseUrl}$path',
    );

    return _sendWithAuthentication(
      request: (accessToken) {
        return http
            .patch(
          uri,
          headers: {
            'Accept': 'application/json',
            'Content-Type':
            'application/json; charset=UTF-8',
            'Authorization': 'Bearer $accessToken',
          },
          body: jsonEncode(body),
        )
            .timeout(
          const Duration(seconds: 10),
        );
      },
    );
  }

  static Future<http.Response> _sendWithAuthentication({
    required Future<http.Response> Function(
        String accessToken,
        ) request,
  }) async {
    final String? accessToken =
    await TokenStorage.getAccessToken();

    if (accessToken == null ||
        accessToken.isEmpty) {
      throw const AuthenticatedApiException(
        '로그인이 필요합니다.',
        unauthorized: true,
      );
    }

    late http.Response response;

    try {
      response = await request(
        accessToken,
      );
    } catch (_) {
      throw const AuthenticatedApiException(
        '서버에 연결할 수 없습니다.',
      );
    }

    if (response.statusCode != 401) {
      return response;
    }

    final String? refreshToken =
    await TokenStorage.getRefreshToken();

    if (refreshToken == null ||
        refreshToken.isEmpty) {
      await TokenStorage.clearTokens();

      throw const AuthenticatedApiException(
        '로그인이 만료되었습니다.',
        unauthorized: true,
      );
    }

    late AuthToken newToken;

    try {
      newToken = await AuthApi.reissue(
        refreshToken: refreshToken,
      );
    } on AuthApiException catch (exception) {
      if (exception.unauthorized) {
        await TokenStorage.clearTokens();

        throw const AuthenticatedApiException(
          '로그인이 만료되었습니다.',
          unauthorized: true,
        );
      }

      throw AuthenticatedApiException(
        exception.message,
      );
    }

    await TokenStorage.saveTokens(
      accessToken: newToken.accessToken,
      refreshToken: newToken.refreshToken,
    );

    try {
      response = await request(
        newToken.accessToken,
      );
    } catch (_) {
      throw const AuthenticatedApiException(
        '서버에 연결할 수 없습니다.',
      );
    }

    if (response.statusCode == 401) {
      await TokenStorage.clearTokens();

      throw const AuthenticatedApiException(
        '로그인이 만료되었습니다.',
        unauthorized: true,
      );
    }

    return response;
  }
}