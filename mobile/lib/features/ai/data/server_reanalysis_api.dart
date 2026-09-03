import 'dart:convert';

import 'package:http/http.dart' as http;
import 'package:echosnap/core/network/authenticated_api_client.dart';

class ServerReanalysisResult {
  const ServerReanalysisResult({
    required this.imageLogId,
    required this.detected,
    required this.classId,
    required this.label,
    required this.confidence,
    required this.detectionCount,
    required this.modelVersion,
    required this.wasteItemId,
    required this.wasteItemName,
    required this.analysisStatus,
  });

  final int imageLogId;

  final bool detected;

  final int? classId;

  final String? label;

  final double? confidence;

  final int detectionCount;

  final String? modelVersion;

  final int? wasteItemId;

  final String? wasteItemName;

  final String analysisStatus;

  factory ServerReanalysisResult.fromJson(
      Map<String, dynamic> json,
      ) {
    return ServerReanalysisResult(
      imageLogId:
      (json['imageLogId'] as num?)
          ?.toInt() ??
          0,
      detected:
      json['detected'] as bool? ??
          false,
      classId:
      (json['classId'] as num?)
          ?.toInt(),
      label:
      json['label']?.toString(),
      confidence:
      (json['confidence'] as num?)
          ?.toDouble(),
      detectionCount:
      (json['detectionCount'] as num?)
          ?.toInt() ??
          0,
      modelVersion:
      json['modelVersion']
          ?.toString(),
      wasteItemId:
      (json['wasteItemId'] as num?)
          ?.toInt(),
      wasteItemName:
      json['wasteItemName']
          ?.toString(),
      analysisStatus:
      json['analysisStatus']
          ?.toString() ??
          '',
    );
  }
}

class ServerReanalysisApiException
    implements Exception {
  const ServerReanalysisApiException(
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

class ServerReanalysisApi {
  ServerReanalysisApi._();

  static Future<ServerReanalysisResult>
  analyze({
    required int imageLogId,
  }) async {
    if (imageLogId <= 0) {
      throw const ServerReanalysisApiException(
        '이미지 업로드 정보가 올바르지 않습니다.',
      );
    }

    final http.Response response;

    try {
      response =
      await AuthenticatedApiClient.post(
        '/api/images/'
            '$imageLogId'
            '/server-reanalysis',
        timeout: const Duration(
          seconds: 45,
        ),
      );
    } on AuthenticatedApiException catch (
    exception
    ) {
      throw ServerReanalysisApiException(
        exception.message,
        unauthorized:
        exception.unauthorized,
      );
    }

    final Map<String, dynamic> body =
    _decodeBody(
      response,
    );

    if (response.statusCode == 401) {
      throw const ServerReanalysisApiException(
        '로그인이 만료되었습니다.',
        unauthorized: true,
      );
    }

    if (response.statusCode < 200 ||
        response.statusCode >= 300 ||
        body['success'] != true) {
      throw ServerReanalysisApiException(
        body['message'] as String? ??
            '서버 AI 재분석을 완료하지 못했습니다.',
      );
    }

    final dynamic data =
    body['data'];

    if (data is! Map<String, dynamic>) {
      throw const ServerReanalysisApiException(
        '서버 AI 분석 결과가 없습니다.',
      );
    }

    final ServerReanalysisResult result =
    ServerReanalysisResult.fromJson(
      data,
    );

    if (result.imageLogId <= 0) {
      throw const ServerReanalysisApiException(
        '서버 AI 분석 결과가 올바르지 않습니다.',
      );
    }

    return result;
  }

  static Map<String, dynamic> _decodeBody(
      http.Response response,
      ) {
    try {
      final dynamic decoded =
      jsonDecode(
        utf8.decode(
          response.bodyBytes,
        ),
      );

      if (decoded
      is Map<String, dynamic>) {
        return decoded;
      }

      return <String, dynamic>{};
    } catch (_) {
      return <String, dynamic>{};
    }
  }
}