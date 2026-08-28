import 'dart:convert';

import 'package:http/http.dart' as http;
import 'package:smart_recycle/core/network/authenticated_api_client.dart';

class ApartmentSearchItem {
  const ApartmentSearchItem({
    required this.id,
    required this.name,
    required this.roadAddress,
    required this.jibunAddress,
  });

  final int id;
  final String name;
  final String? roadAddress;
  final String? jibunAddress;

  factory ApartmentSearchItem.fromJson(
      Map<String, dynamic> json,
      ) {
    return ApartmentSearchItem(
      id: json['id'] as int? ?? 0,
      name: json['name'] as String? ?? '',
      roadAddress:
      json['roadAddress'] as String?,
      jibunAddress:
      json['jibunAddress'] as String?,
    );
  }

  String get displayAddress {
    if (roadAddress != null &&
        roadAddress!.isNotEmpty) {
      return roadAddress!;
    }

    return jibunAddress ?? '';
  }
}

class AddressSearchItem {
  const AddressSearchItem({
    required this.addressName,
    required this.roadAddress,
    required this.jibunAddress,
    required this.buildingName,
    required this.zoneNo,
    required this.sido,
    required this.sigungu,
    required this.legalDong,
    required this.administrativeDong,
    required this.legalDongCode,
    required this.administrativeDongCode,
    required this.latitude,
    required this.longitude,
  });

  final String addressName;
  final String? roadAddress;
  final String? jibunAddress;
  final String? buildingName;
  final String? zoneNo;

  final String sido;
  final String sigungu;

  final String? legalDong;
  final String? administrativeDong;
  final String? legalDongCode;
  final String? administrativeDongCode;

  final double latitude;
  final double longitude;

  factory AddressSearchItem.fromJson(
      Map<String, dynamic> json,
      ) {
    return AddressSearchItem(
      addressName:
      json['addressName'] as String? ?? '',
      roadAddress:
      json['roadAddress'] as String?,
      jibunAddress:
      json['jibunAddress'] as String?,
      buildingName:
      json['buildingName'] as String?,
      zoneNo:
      json['zoneNo'] as String?,
      sido:
      json['sido'] as String? ?? '',
      sigungu:
      json['sigungu'] as String? ?? '',
      legalDong:
      json['legalDong'] as String?,
      administrativeDong:
      json['administrativeDong'] as String?,
      legalDongCode:
      json['legalDongCode'] as String?,
      administrativeDongCode:
      json['administrativeDongCode'] as String?,
      latitude: _toDouble(
        json['latitude'],
      ),
      longitude: _toDouble(
        json['longitude'],
      ),
    );
  }

  static double _toDouble(
      dynamic value,
      ) {
    if (value is num) {
      return value.toDouble();
    }

    return double.tryParse(
      value?.toString() ?? '',
    ) ??
        0;
  }

  Map<String, dynamic> toSaveRequest() {
    return {
      'addressName': addressName,
      'roadAddress': roadAddress,
      'jibunAddress': jibunAddress,
      'buildingName': buildingName,
      'zoneNo': zoneNo,
      'sido': sido,
      'sigungu': sigungu,
      'legalDong': legalDong,
      'administrativeDong':
      administrativeDong,
      'legalDongCode': legalDongCode,
      'administrativeDongCode':
      administrativeDongCode,
      'latitude': latitude,
      'longitude': longitude,
    };
  }

  String get displayAddress {
    if (roadAddress != null &&
        roadAddress!.isNotEmpty) {
      return roadAddress!;
    }

    if (jibunAddress != null &&
        jibunAddress!.isNotEmpty) {
      return jibunAddress!;
    }

    return addressName;
  }
}

class ResidenceSetupApiException
    implements Exception {
  const ResidenceSetupApiException(
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

class ResidenceSetupApi {
  ResidenceSetupApi._();

  static Future<List<ApartmentSearchItem>>
  searchApartments(
      String keyword,
      ) async {
    final http.Response response;

    try {
      response =
      await AuthenticatedApiClient.get(
        '/api/apartments',
        queryParameters: {
          'keyword': keyword.trim(),
          'page': '0',
          'size': '20',
        },
      );
    } on AuthenticatedApiException catch (exception) {
      throw ResidenceSetupApiException(
        exception.message,
        unauthorized: exception.unauthorized,
      );
    }

    final Map<String, dynamic> body =
    _decodeResponse(response);

    _validateSuccess(
      response,
      body,
    );

    final dynamic data = body['data'];

    if (data is! Map<String, dynamic>) {
      return const [];
    }

    final dynamic content = data['content'];

    if (content is! List) {
      return const [];
    }

    return content
        .whereType<Map<String, dynamic>>()
        .map(
      ApartmentSearchItem.fromJson,
    )
        .toList();
  }

  static Future<List<AddressSearchItem>>
  searchAddresses(
      String query,
      ) async {
    final http.Response response;

    try {
      response =
      await AuthenticatedApiClient.get(
        '/api/addresses/search',
        queryParameters: {
          'query': query.trim(),
          'page': '1',
          'size': '10',
        },
      );
    } on AuthenticatedApiException catch (exception) {
      throw ResidenceSetupApiException(
        exception.message,
        unauthorized: exception.unauthorized,
      );
    }

    final Map<String, dynamic> body =
    _decodeResponse(response);

    _validateSuccess(
      response,
      body,
    );

    final dynamic data = body['data'];

    if (data is! List) {
      return const [];
    }

    return data
        .whereType<Map<String, dynamic>>()
        .map(
      AddressSearchItem.fromJson,
    )
        .toList();
  }

  static Future<void> saveApartment(
      int apartmentId,
      ) async {
    await _patch(
      '/api/users/me/apartment',
      {
        'apartmentId': apartmentId,
      },
    );
  }

  static Future<void> saveResidence(
      AddressSearchItem address,
      ) async {
    await _patch(
      '/api/users/me/residence',
      address.toSaveRequest(),
    );
  }

  static Future<void> completeOnboarding() async {
    await _patch(
      '/api/users/me/onboarding',
      {
        'completed': true,
      },
    );
  }

  static Future<void> _patch(
      String path,
      Map<String, dynamic> requestBody,
      ) async {
    final http.Response response;

    try {
      response =
      await AuthenticatedApiClient.patch(
        path,
        body: requestBody,
      );
    } on AuthenticatedApiException catch (exception) {
      throw ResidenceSetupApiException(
        exception.message,
        unauthorized: exception.unauthorized,
      );
    }

    final Map<String, dynamic> body =
    _decodeResponse(response);

    _validateSuccess(
      response,
      body,
    );
  }

  static Map<String, dynamic> _decodeResponse(
      http.Response response,
      ) {
    try {
      return jsonDecode(
        utf8.decode(response.bodyBytes),
      ) as Map<String, dynamic>;
    } catch (_) {
      throw ResidenceSetupApiException(
        '서버 응답을 처리할 수 없습니다. '
            '(HTTP ${response.statusCode})',
      );
    }
  }

  static void _validateSuccess(
      http.Response response,
      Map<String, dynamic> body,
      ) {
    if (response.statusCode == 401) {
      throw const ResidenceSetupApiException(
        '로그인이 만료되었습니다.',
        unauthorized: true,
      );
    }

    final String message =
        body['message'] as String? ??
            '요청 처리 중 오류가 발생했습니다.';

    if (response.statusCode < 200 ||
        response.statusCode >= 300 ||
        body['success'] != true) {
      throw ResidenceSetupApiException(
        message,
      );
    }
  }
}