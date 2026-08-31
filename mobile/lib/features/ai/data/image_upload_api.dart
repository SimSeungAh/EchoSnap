import 'dart:convert';
import 'dart:typed_data';

import 'package:http/http.dart' as http;
import 'package:smart_recycle/core/network/authenticated_api_client.dart';

class ImageUploadResult {
  const ImageUploadResult({
    required this.imageLogId,
    required this.imageUrl,
    required this.originalFileName,
    required this.contentType,
    required this.fileSize,
    required this.analysisStatus,
    required this.createdAt,
  });

  final int imageLogId;

  final String imageUrl;
  final String originalFileName;
  final String contentType;

  final int fileSize;

  final String analysisStatus;
  final String? createdAt;

  factory ImageUploadResult.fromJson(
      Map<String, dynamic> json,
      ) {
    return ImageUploadResult(
      imageLogId:
      (json['imageLogId'] as num?)
          ?.toInt() ??
          0,
      imageUrl:
      json['imageUrl'] as String? ??
          '',
      originalFileName:
      json['originalFileName']
      as String? ??
          '',
      contentType:
      json['contentType'] as String? ??
          '',
      fileSize:
      (json['fileSize'] as num?)
          ?.toInt() ??
          0,
      analysisStatus:
      json['analysisStatus']
          ?.toString() ??
          '',
      createdAt:
      json['createdAt']?.toString(),
    );
  }
}

class ImageUploadApiException
    implements Exception {
  const ImageUploadApiException(
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

class ImageUploadApi {
  ImageUploadApi._();

  static const int _maxFileSize =
      10 * 1024 * 1024;

  static Future<ImageUploadResult>
  upload({
    required Uint8List bytes,
    required String fileName,
  }) async {
    if (bytes.isEmpty) {
      throw const ImageUploadApiException(
        '선택한 이미지가 비어 있습니다.',
      );
    }

    if (bytes.lengthInBytes >
        _maxFileSize) {
      throw const ImageUploadApiException(
        '이미지는 10MB 이하만 업로드할 수 있습니다.',
      );
    }

    final String contentType =
    _detectContentType(
      bytes,
    );

    final http.Response response;

    try {
      response =
      await AuthenticatedApiClient
          .multipartPost(
        '/api/images',
        fieldName: 'file',
        bytes: bytes,
        fileName: _normalizeFileName(
          fileName,
          contentType,
        ),
        contentType: contentType,
      );
    } on AuthenticatedApiException catch (
    exception
    ) {
      throw ImageUploadApiException(
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
      throw const ImageUploadApiException(
        '로그인이 만료되었습니다.',
        unauthorized: true,
      );
    }

    if (response.statusCode < 200 ||
        response.statusCode >= 300 ||
        body['success'] != true) {
      throw ImageUploadApiException(
        body['message'] as String? ??
            '이미지를 업로드하지 못했습니다.',
      );
    }

    final dynamic data =
    body['data'];

    if (data is! Map<String, dynamic>) {
      throw const ImageUploadApiException(
        '이미지 업로드 결과가 없습니다.',
      );
    }

    final ImageUploadResult result =
    ImageUploadResult.fromJson(
      data,
    );

    if (result.imageLogId <= 0) {
      throw const ImageUploadApiException(
        '이미지 업로드 ID를 확인할 수 없습니다.',
      );
    }

    return result;
  }

  /**
   * 확장자나 브라우저의 MIME 정보만 믿지 않고
   * 실제 이미지의 시작 바이트를 기준으로
   * JPEG / PNG를 판별합니다.
   *
   * 백엔드 역시 같은 방식으로 Magic Number를
   * 검증하므로 Flutter와 서버의 규칙을 맞춥니다.
   */
  static String _detectContentType(
      Uint8List bytes,
      ) {
    if (_isJpeg(bytes)) {
      return 'image/jpeg';
    }

    if (_isPng(bytes)) {
      return 'image/png';
    }

    throw const ImageUploadApiException(
      'JPG, JPEG, PNG 이미지만 사용할 수 있습니다.',
    );
  }

  static bool _isJpeg(
      Uint8List bytes,
      ) {
    return bytes.length >= 3 &&
        bytes[0] == 0xFF &&
        bytes[1] == 0xD8 &&
        bytes[2] == 0xFF;
  }

  static bool _isPng(
      Uint8List bytes,
      ) {
    return bytes.length >= 8 &&
        bytes[0] == 0x89 &&
        bytes[1] == 0x50 &&
        bytes[2] == 0x4E &&
        bytes[3] == 0x47 &&
        bytes[4] == 0x0D &&
        bytes[5] == 0x0A &&
        bytes[6] == 0x1A &&
        bytes[7] == 0x0A;
  }

  /**
   * 브라우저에서 전달되는 파일명이 이상하거나
   * 확장자가 없는 경우에도 서버에 안전한 이름을 전달합니다.
   *
   * 실제 서버 저장 파일명은 백엔드에서 UUID로
   * 다시 생성하기 때문에 이 이름은 표시/추적용입니다.
   */
  static String _normalizeFileName(
      String fileName,
      String contentType,
      ) {
    String normalized =
    fileName.trim();

    if (normalized.isEmpty) {
      return contentType ==
          'image/png'
          ? 'image.png'
          : 'image.jpg';
    }

    normalized =
        normalized
            .replaceAll(
          '\\',
          '/',
        )
            .split('/')
            .last;

    final String lower =
    normalized.toLowerCase();

    if (contentType ==
        'image/png') {
      if (!lower.endsWith('.png')) {
        normalized =
        '$normalized.png';
      }
    } else {
      if (!lower.endsWith('.jpg') &&
          !lower.endsWith('.jpeg')) {
        normalized =
        '$normalized.jpg';
      }
    }

    return normalized;
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