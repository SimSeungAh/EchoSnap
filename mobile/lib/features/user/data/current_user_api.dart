import 'dart:convert';

import 'package:http/http.dart' as http;
import 'package:smart_recycle/core/network/authenticated_api_client.dart';

class CurrentUserApartment {
  const CurrentUserApartment({
    required this.id,
    required this.name,
    this.roadAddress,
    this.jibunAddress,
  });

  final int id;
  final String name;
  final String? roadAddress;
  final String? jibunAddress;

  factory CurrentUserApartment.fromJson(
      Map<String, dynamic> json,
      ) {
    return CurrentUserApartment(
      id: (json['id'] as num?)?.toInt() ?? 0,
      name: json['name'] as String? ?? '',
      roadAddress: json['roadAddress'] as String?,
      jibunAddress: json['jibunAddress'] as String?,
    );
  }
}

class CurrentUserResidence {
  const CurrentUserResidence({
    required this.id,
    this.addressName,
    this.roadAddress,
    this.jibunAddress,
    this.buildingName,
    this.zoneNo,
    this.sido,
    this.sigungu,
    this.legalDong,
    this.administrativeDong,
  });

  final int id;

  final String? addressName;
  final String? roadAddress;
  final String? jibunAddress;
  final String? buildingName;
  final String? zoneNo;

  final String? sido;
  final String? sigungu;
  final String? legalDong;
  final String? administrativeDong;

  factory CurrentUserResidence.fromJson(
      Map<String, dynamic> json,
      ) {
    return CurrentUserResidence(
      id: (json['id'] as num?)?.toInt() ?? 0,
      addressName: json['addressName'] as String?,
      roadAddress: json['roadAddress'] as String?,
      jibunAddress: json['jibunAddress'] as String?,
      buildingName: json['buildingName'] as String?,
      zoneNo: json['zoneNo'] as String?,
      sido: json['sido'] as String?,
      sigungu: json['sigungu'] as String?,
      legalDong: json['legalDong'] as String?,
      administrativeDong:
      json['administrativeDong'] as String?,
    );
  }

  String get displayAddress {
    final candidates = <String?>[
      roadAddress,
      addressName,
      jibunAddress,
    ];

    for (final value in candidates) {
      if (value != null &&
          value.trim().isNotEmpty) {
        return value.trim();
      }
    }

    return '주소 정보 없음';
  }
}

class CurrentUser {
  const CurrentUser({
    required this.id,
    required this.email,
    required this.nickname,
    required this.role,
    required this.status,
    required this.residenceType,
    required this.apartment,
    required this.residence,
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

  final CurrentUserApartment? apartment;
  final CurrentUserResidence? residence;

  final bool notificationEnabled;
  final bool locationEnabled;
  final bool onboardingCompleted;

  factory CurrentUser.fromJson(
      Map<String, dynamic> json,
      ) {
    final dynamic apartmentJson =
    json['apartment'];

    final dynamic residenceJson =
    json['residence'];

    return CurrentUser(
      id: (json['id'] as num?)?.toInt() ?? 0,
      email: json['email'] as String? ?? '',
      nickname:
      json['nickname'] as String? ?? '',
      role: json['role'] as String? ?? '',
      status: json['status'] as String? ?? '',
      residenceType:
      json['residenceType'] as String?,
      apartment:
      apartmentJson is Map<String, dynamic>
          ? CurrentUserApartment.fromJson(
        apartmentJson,
      )
          : null,
      residence:
      residenceJson is Map<String, dynamic>
          ? CurrentUserResidence.fromJson(
        residenceJson,
      )
          : null,
      notificationEnabled:
      json['notificationEnabled'] as bool? ??
          false,
      locationEnabled:
      json['locationEnabled'] as bool? ??
          false,
      onboardingCompleted:
      json['onboardingCompleted'] as bool? ??
          false,
    );
  }
}

class CurrentUserApiException
    implements Exception {
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
    } on AuthenticatedApiException catch (
    exception
    ) {
      throw CurrentUserApiException(
        exception.message,
        unauthorized:
        exception.unauthorized,
      );
    }

    Map<String, dynamic> body;

    try {
      final dynamic decoded =
      jsonDecode(
        utf8.decode(
          response.bodyBytes,
        ),
      );

      if (decoded
      is! Map<String, dynamic>) {
        throw const FormatException();
      }

      body = decoded;
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