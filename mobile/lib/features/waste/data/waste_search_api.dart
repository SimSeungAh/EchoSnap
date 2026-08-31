import 'dart:convert';

import 'package:http/http.dart' as http;
import 'package:smart_recycle/core/network/authenticated_api_client.dart';

class WasteCategoryItem {
  const WasteCategoryItem({
    required this.id,
    required this.code,
    required this.name,
    this.description,
    required this.sortOrder,
  });

  final int id;
  final String code;
  final String name;
  final String? description;
  final int sortOrder;

  factory WasteCategoryItem.fromJson(
      Map<String, dynamic> json,
      ) {
    return WasteCategoryItem(
      id: (json['id'] as num?)?.toInt() ?? 0,
      code: json['code'] as String? ?? '',
      name: json['name'] as String? ?? '',
      description: json['description'] as String?,
      sortOrder:
      (json['sortOrder'] as num?)?.toInt() ?? 0,
    );
  }
}

class WasteCategorySummary {
  const WasteCategorySummary({
    required this.id,
    required this.code,
    required this.name,
  });

  final int id;
  final String code;
  final String name;

  factory WasteCategorySummary.fromJson(
      Map<String, dynamic> json,
      ) {
    return WasteCategorySummary(
      id: (json['id'] as num?)?.toInt() ?? 0,
      code: json['code'] as String? ?? '',
      name: json['name'] as String? ?? '',
    );
  }
}

class WasteSearchItem {
  const WasteSearchItem({
    required this.id,
    required this.name,
    this.imageUrl,
    required this.category,
  });

  final int id;
  final String name;
  final String? imageUrl;
  final WasteCategorySummary category;

  factory WasteSearchItem.fromJson(
      Map<String, dynamic> json,
      ) {
    final categoryJson = json['category'];

    return WasteSearchItem(
      id: (json['id'] as num?)?.toInt() ?? 0,
      name: json['name'] as String? ?? '',
      imageUrl: json['imageUrl'] as String?,
      category: WasteCategorySummary.fromJson(
        categoryJson is Map<String, dynamic>
            ? categoryJson
            : <String, dynamic>{},
      ),
    );
  }
}

class WasteSearchResult {
  const WasteSearchResult({
    required this.items,
    required this.page,
    required this.size,
    required this.totalElements,
    required this.totalPages,
    required this.first,
    required this.last,
  });

  final List<WasteSearchItem> items;
  final int page;
  final int size;
  final int totalElements;
  final int totalPages;
  final bool first;
  final bool last;
}

class WasteSearchApiException implements Exception {
  const WasteSearchApiException(
      this.message, {
        this.unauthorized = false,
      });

  final String message;
  final bool unauthorized;

  @override
  String toString() => message;
}

class WasteSearchApi {
  WasteSearchApi._();

  static Future<List<WasteCategoryItem>>
  getCategories() async {
    final http.Response response;

    try {
      response = await AuthenticatedApiClient.get(
        '/api/waste/categories',
      );
    } on AuthenticatedApiException catch (exception) {
      throw WasteSearchApiException(
        exception.message,
        unauthorized: exception.unauthorized,
      );
    }

    final body = _decodeBody(response);

    _validateResponse(
      response,
      body,
    );

    final data = body['data'];

    if (data is! List) {
      return const [];
    }

    return data
        .whereType<Map<String, dynamic>>()
        .map(WasteCategoryItem.fromJson)
        .toList();
  }

  static Future<WasteSearchResult> searchItems({
    required String keyword,
    int page = 0,
    int size = 20,
    int? categoryId,
  }) async {
    final queryParameters = <String, String>{
      'keyword': keyword.trim(),
      'page': page.toString(),
      'size': size.toString(),
      'sort': 'name,asc',
    };

    if (categoryId != null) {
      queryParameters['categoryId'] =
          categoryId.toString();
    }

    final http.Response response;

    try {
      response = await AuthenticatedApiClient.get(
        '/api/waste/items',
        queryParameters: queryParameters,
      );
    } on AuthenticatedApiException catch (exception) {
      throw WasteSearchApiException(
        exception.message,
        unauthorized: exception.unauthorized,
      );
    }

    final body = _decodeBody(response);

    _validateResponse(
      response,
      body,
    );

    final data = body['data'];

    if (data is! Map<String, dynamic>) {
      return const WasteSearchResult(
        items: [],
        page: 0,
        size: 20,
        totalElements: 0,
        totalPages: 0,
        first: true,
        last: true,
      );
    }

    final content = data['content'];

    final items = content is List
        ? content
        .whereType<Map<String, dynamic>>()
        .map(WasteSearchItem.fromJson)
        .toList()
        : <WasteSearchItem>[];

    return WasteSearchResult(
      items: items,
      page: (data['page'] as num?)?.toInt() ?? 0,
      size: (data['size'] as num?)?.toInt() ?? size,
      totalElements:
      (data['totalElements'] as num?)?.toInt() ??
          0,
      totalPages:
      (data['totalPages'] as num?)?.toInt() ?? 0,
      first: data['first'] as bool? ?? true,
      last: data['last'] as bool? ?? true,
    );
  }

  static Map<String, dynamic> _decodeBody(
      http.Response response,
      ) {
    try {
      final decoded = jsonDecode(
        utf8.decode(response.bodyBytes),
      );

      if (decoded is Map<String, dynamic>) {
        return decoded;
      }

      return <String, dynamic>{};
    } catch (_) {
      return <String, dynamic>{};
    }
  }

  static void _validateResponse(
      http.Response response,
      Map<String, dynamic> body,
      ) {
    if (response.statusCode == 401) {
      throw const WasteSearchApiException(
        '로그인이 만료되었습니다.',
        unauthorized: true,
      );
    }

    if (response.statusCode < 200 ||
        response.statusCode >= 300 ||
        body['success'] != true) {
      throw WasteSearchApiException(
        body['message'] as String? ??
            '품목 정보를 불러오지 못했습니다.',
      );
    }
  }
}