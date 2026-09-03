import 'dart:convert';

import 'package:http/http.dart' as http;
import 'package:echosnap/core/network/authenticated_api_client.dart';

class ImageCorrectionResult {
  const ImageCorrectionResult({
    required this.imageLogId,
    required this.originalAiWasteItemId,
    required this.originalAiWasteItemName,
    required this.correctedWasteItemId,
    required this.correctedWasteItemName,
    required this.analysisStatus,
    required this.reviewStatus,
    required this.correctedAt,
  });

  final int imageLogId;

  final int? originalAiWasteItemId;

  final String? originalAiWasteItemName;

  final int correctedWasteItemId;

  final String correctedWasteItemName;

  final String analysisStatus;

  final String reviewStatus;

  final DateTime? correctedAt;

  factory ImageCorrectionResult.fromJson(
      Map<String, dynamic> json,
      ) {
    return ImageCorrectionResult(
      imageLogId:
      (json['imageLogId'] as num?)
          ?.toInt() ??
          0,

      originalAiWasteItemId:
      (json['originalAiWasteItemId']
      as num?)
          ?.toInt(),

      originalAiWasteItemName:
      json['originalAiWasteItemName']
          ?.toString(),

      correctedWasteItemId:
      (json['correctedWasteItemId']
      as num?)
          ?.toInt() ??
          0,

      correctedWasteItemName:
      json['correctedWasteItemName']
          ?.toString() ??
          '',

      analysisStatus:
      json['analysisStatus']
          ?.toString() ??
          '',

      reviewStatus:
      json['reviewStatus']
          ?.toString() ??
          '',

      correctedAt:
      _parseDateTime(
        json['correctedAt'],
      ),
    );
  }

  static DateTime? _parseDateTime(
      dynamic value,
      ) {
    if (value == null) {
      return null;
    }

    return DateTime.tryParse(
      value.toString(),
    );
  }
}

class ImageCorrectionApiException
    implements Exception {
  const ImageCorrectionApiException(
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

class ImageCorrectionApi {
  ImageCorrectionApi._();

  /// AI가 추정한 품목이 틀렸을 때
  /// 사용자가 선택한 올바른 WasteItem을 저장합니다.
  ///
  /// 기존 모바일/서버 AI 결과를 덮어쓰지 않고
  /// ImageLog의 userCorrectedWasteItem에
  /// 별도로 저장됩니다.
  static Future<ImageCorrectionResult> correct({
    required int imageLogId,
    required int wasteItemId,
    required String description,
  }) async {
    if (imageLogId <= 0) {
      throw const ImageCorrectionApiException(
        'AI 분석 이미지 정보가 올바르지 않습니다.',
      );
    }

    if (wasteItemId <= 0) {
      throw const ImageCorrectionApiException(
        '수정할 품목 정보가 올바르지 않습니다.',
      );
    }

    final http.Response response;

    try {
      response =
      await AuthenticatedApiClient.put(
        '/api/images/'
            '$imageLogId'
            '/correction',
        body: <String, dynamic>{
          'wasteItemId':
          wasteItemId,
          'description':
          description.trim().isEmpty
              ? null
              : description.trim(),
        },
      );
    } on AuthenticatedApiException catch (
    exception
    ) {
      throw ImageCorrectionApiException(
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
      throw const ImageCorrectionApiException(
        '로그인이 만료되었습니다.',
        unauthorized: true,
      );
    }

    if (response.statusCode < 200 ||
        response.statusCode >= 300 ||
        body['success'] != true) {
      throw ImageCorrectionApiException(
        body['message']
            ?.toString() ??
            'AI 분석 결과를 수정하지 못했습니다.',
      );
    }

    final dynamic data =
    body['data'];

    if (data is! Map<String, dynamic>) {
      throw const ImageCorrectionApiException(
        '수정된 AI 결과를 확인하지 못했습니다.',
      );
    }

    final ImageCorrectionResult result =
    ImageCorrectionResult.fromJson(
      data,
    );

    if (result.imageLogId <= 0 ||
        result.correctedWasteItemId <= 0 ||
        result.correctedWasteItemName.isEmpty) {
      throw const ImageCorrectionApiException(
        '수정된 품목 정보가 올바르지 않습니다.',
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
