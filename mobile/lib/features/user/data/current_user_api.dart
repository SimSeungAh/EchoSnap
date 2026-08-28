import 'dart:convert';

import 'package:http/http.dart' as http;
import 'package:smart_recycle/core/network/authenticated_api_client.dart';

class CurrentUser {
  const CurrentUser({
    required this.id,
    required this.email,
    required this.nickname,
    required this.role,
    required this.status,
    required this.residenceType,
    required this.notificationEnabled,
    required this.locationEnabled,
    required this.onboardingCompleted,
  });

  final int id;
  final String email;
  final String nickname;
  final String role;
  final String status;

  final String? residenceType;

  final bool notificationEnabled;
  final bool locationEnabled;
  final bool onboardingCompleted;

  factory CurrentUser.fromJson(
      Map<String, dynamic> json,
      ) {
    return CurrentUser(
      id: json['id'] as int? ?? 0,
      email: json['email'] as String? ?? '',
      nickname: json['nickname'] as String? ?? '',
      role: json['role'] as String? ?? '',
      status: json['status'] as String? ?? '',
      residenceType:
      json['residenceType'] as String?,
      notificationEnabled:
      json['notificationEnabled'] as bool? ?? false,
      locationEnabled:
      json['locationEnabled'] as bool? ?? false,
      onboardingCompleted:
      json['onboardingCompleted'] as bool? ?? false,
    );
  }
}

class CurrentUserApiException implements Exception {
  const CurrentUserApiException(
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

class CurrentUserApi {
  CurrentUserApi._();

  static Future<CurrentUser> getMe() async {
    late http.Response response;

    try {
      response =
      await AuthenticatedApiClient.get(
        '/api/users/me',
      );
    } on AuthenticatedApiException catch (exception) {
      throw CurrentUserApiException(
        exception.message,
        unauthorized: exception.unauthorized,
      );
    }

    Map<String, dynamic> body;

    try {
      body = jsonDecode(
        utf8.decode(response.bodyBytes),
      ) as Map<String, dynamic>;
    } catch (_) {
      throw CurrentUserApiException(
        '사용자 정보를 처리할 수 없습니다. '
            '(HTTP ${response.statusCode})',
      );
    }

    if (response.statusCode == 401) {
      throw const CurrentUserApiException(
        '로그인이 만료되었습니다.',
        unauthorized: true,
      );
    }

    final String message =
        body['message'] as String? ??
            '사용자 정보를 불러오지 못했습니다.';

    if (response.statusCode < 200 ||
        response.statusCode >= 300) {
      throw CurrentUserApiException(
        message,
      );
    }

    if (body['success'] != true) {
      throw CurrentUserApiException(
        message,
      );
    }

    final dynamic data = body['data'];

    if (data is! Map<String, dynamic>) {
      throw const CurrentUserApiException(
        '사용자 정보가 없습니다.',
      );
    }

    return CurrentUser.fromJson(
      data,
    );
  }
}