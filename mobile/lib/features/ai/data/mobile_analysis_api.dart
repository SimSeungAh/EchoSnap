import 'dart:convert';

import 'package:http/http.dart' as http;
import 'package:smart_recycle/core/network/authenticated_api_client.dart';

/// Flutter TFLite 1차 분석 결과를
/// Spring Boot에 저장한 뒤 반환되는 결과입니다.
class MobileAnalysisResult {
  const MobileAnalysisResult({
    required this.imageLogId,
    required this.wasteItemId,
    required this.wasteItemName,
    required this.confidence,
    required this.modelVersion,
    required this.analysisStatus,
    required this.needsServerReanalysis,
  });

  final int imageLogId;

  final int? wasteItemId;

  final String? wasteItemName;

  final double? confidence;

  final String? modelVersion;

  final String analysisStatus;

  final bool needsServerReanalysis;

  factory MobileAnalysisResult.fromJson(
      Map<String, dynamic> json,
      ) {
    return MobileAnalysisResult(
      imageLogId:
      (json['imageLogId'] as num?)
          ?.toInt() ??
          0,
      wasteItemId:
      (json['wasteItemId'] as num?)
          ?.toInt(),
      wasteItemName:
      json['wasteItemName']
          ?.toString(),
      confidence:
      (json['confidence'] as num?)
          ?.toDouble(),
      modelVersion:
      json['modelVersion']
          ?.toString(),
      analysisStatus:
      json['analysisStatus']
          ?.toString() ??
          '',
      needsServerReanalysis:
      json['needsServerReanalysis']
      as bool? ??
          false,
    );
  }
}

class MobileAnalysisApiException
    implements Exception {
  const MobileAnalysisApiException(
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

class MobileAnalysisApi {
  MobileAnalysisApi._();

  /// Flutter 기기에서 수행한
  /// 실제 TFLite 분석 결과를 저장합니다.
  ///
  /// classId는 전달하지 않습니다.
  ///
  /// 모델이 반환한 label을
  /// Spring Boot가 AiWasteItemMapping을 통해
  /// WasteItem으로 연결합니다.
  static Future<MobileAnalysisResult>
  record({
    required int imageLogId,
    required String modelLabel,
    required double confidence,
    required String modelVersion,
  }) async {
    if (imageLogId <= 0) {
      throw const MobileAnalysisApiException(
        '이미지 업로드 정보가 올바르지 않습니다.',
      );
    }

    final String normalizedLabel =
    modelLabel.trim();

    if (normalizedLabel.isEmpty) {
      throw const MobileAnalysisApiException(
        'AI 모델 라벨이 없습니다.',
      );
    }

    final String normalizedVersion =
    modelVersion.trim();

    if (normalizedVersion.isEmpty) {
      throw const MobileAnalysisApiException(
        'AI 모델 버전이 없습니다.',
      );
    }

    if (confidence < 0 ||
        confidence > 1) {
      throw const MobileAnalysisApiException(
        'AI 신뢰도 값이 올바르지 않습니다.',
      );
    }

    final http.Response response;

    try {
      response =
      await AuthenticatedApiClient
          .post(
        '/api/images/'
            '$imageLogId'
            '/mobile-analysis',
        body: <String, dynamic>{
          'modelLabel':
          normalizedLabel,
          'confidence':
          confidence,
          'modelVersion':
          normalizedVersion,
        },
      );
    } on AuthenticatedApiException catch (
    exception
    ) {
      throw MobileAnalysisApiException(
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
      throw const MobileAnalysisApiException(
        '로그인이 만료되었습니다.',
        unauthorized: true,
      );
    }

    if (response.statusCode < 200 ||
        response.statusCode >= 300 ||
        body['success'] != true) {
      throw MobileAnalysisApiException(
        body['message'] as String? ??
            '모바일 AI 결과를 저장하지 못했습니다.',
      );
    }

    final dynamic data =
    body['data'];

    if (data is! Map<String, dynamic>) {
      throw const MobileAnalysisApiException(
        '모바일 AI 분석 결과가 없습니다.',
      );
    }

    final MobileAnalysisResult result =
    MobileAnalysisResult.fromJson(
      data,
    );

    if (result.imageLogId <= 0) {
      throw const MobileAnalysisApiException(
        '모바일 AI 분석 결과가 올바르지 않습니다.',
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