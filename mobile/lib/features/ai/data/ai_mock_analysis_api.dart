import 'dart:convert';

import 'package:http/http.dart' as http;
import 'package:echosnap/core/network/authenticated_api_client.dart';

enum MockAnalysisScenario {
  highConfidence,
  lowConfidence,
  analysisFailed;

  String get apiValue {
    return switch (this) {
      MockAnalysisScenario
          .highConfidence =>
      'HIGH_CONFIDENCE',

      MockAnalysisScenario
          .lowConfidence =>
      'LOW_CONFIDENCE',

      MockAnalysisScenario
          .analysisFailed =>
      'ANALYSIS_FAILED',
    };
  }

  String get label {
    return switch (this) {
      MockAnalysisScenario
          .highConfidence =>
      '높은 신뢰도 · 92%',

      MockAnalysisScenario
          .lowConfidence =>
      '낮은 신뢰도 · 45%',

      MockAnalysisScenario
          .analysisFailed =>
      '분석 실패',
    };
  }

  bool get requiresWasteItem {
    return this !=
        MockAnalysisScenario
            .analysisFailed;
  }
}

class MockAnalysisResult {
  const MockAnalysisResult({
    required this.imageLogId,
    required this.scenario,
    required this.wasteItemId,
    required this.wasteItemName,
    required this.confidence,
    required this.modelVersion,
    required this.analysisStatus,
    required this.needsServerReanalysis,
  });

  final int imageLogId;

  final String scenario;

  final int? wasteItemId;
  final String? wasteItemName;

  final double? confidence;

  final String? modelVersion;

  final String analysisStatus;

  final bool needsServerReanalysis;

  factory MockAnalysisResult.fromJson(
      Map<String, dynamic> json,
      ) {
    return MockAnalysisResult(
      imageLogId:
      (json['imageLogId'] as num?)
          ?.toInt() ??
          0,
      scenario:
      json['scenario']?.toString() ??
          '',
      wasteItemId:
      (json['wasteItemId'] as num?)
          ?.toInt(),
      wasteItemName:
      json['wasteItemName']
      as String?,
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

class AiMockAnalysisApiException
    implements Exception {
  const AiMockAnalysisApiException(
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

class AiMockAnalysisApi {
  AiMockAnalysisApi._();

  static Future<MockAnalysisResult>
  analyze({
    required int imageLogId,
    required MockAnalysisScenario
    scenario,
    int? wasteItemId,
  }) async {
    if (imageLogId <= 0) {
      throw const AiMockAnalysisApiException(
        '이미지 업로드 정보가 올바르지 않습니다.',
      );
    }

    if (scenario.requiresWasteItem &&
        wasteItemId == null) {
      throw const AiMockAnalysisApiException(
        '테스트할 폐기물 품목을 선택해주세요.',
      );
    }

    final Map<String, dynamic> requestBody =
    {
      'wasteItemId':
      scenario.requiresWasteItem
          ? wasteItemId
          : null,
      'scenario':
      scenario.apiValue,
    };

    final http.Response response;

    try {
      response =
      await AuthenticatedApiClient
          .post(
        '/api/images/$imageLogId/mock-analysis',
        body: requestBody,
      );
    } on AuthenticatedApiException catch (
    exception
    ) {
      throw AiMockAnalysisApiException(
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
      throw const AiMockAnalysisApiException(
        '로그인이 만료되었습니다.',
        unauthorized: true,
      );
    }

    if (response.statusCode < 200 ||
        response.statusCode >= 300 ||
        body['success'] != true) {
      throw AiMockAnalysisApiException(
        body['message'] as String? ??
            '개발용 AI 분석을 실행하지 못했습니다.',
      );
    }

    final dynamic data =
    body['data'];

    if (data is! Map<String, dynamic>) {
      throw const AiMockAnalysisApiException(
        'AI 분석 결과가 없습니다.',
      );
    }

    return MockAnalysisResult
        .fromJson(
      data,
    );
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