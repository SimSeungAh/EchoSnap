import 'dart:convert';

import 'package:http/http.dart' as http;
import 'package:echosnap/core/network/authenticated_api_client.dart';

class WasteGuideCheckItem {
  const WasteGuideCheckItem({
    required this.id,
    required this.content,
    required this.sortOrder,
    required this.requiredItem,
  });

  final int id;
  final String content;
  final int sortOrder;
  final bool requiredItem;

  factory WasteGuideCheckItem.fromJson(
      Map<String, dynamic> json,
      ) {
    return WasteGuideCheckItem(
      id: (json['id'] as num?)?.toInt() ?? 0,
      content: json['content'] as String? ?? '',
      sortOrder:
      (json['sortOrder'] as num?)?.toInt() ?? 0,
      requiredItem:
      json['required'] as bool? ?? false,
    );
  }
}

class WasteGuideDetail {
  const WasteGuideDetail({
    required this.id,
    required this.summary,
    required this.disposalMethod,
    required this.caution,
    required this.checkItems,
  });

  final int id;
  final String? summary;
  final String? disposalMethod;
  final String? caution;
  final List<WasteGuideCheckItem> checkItems;

  factory WasteGuideDetail.fromJson(
      Map<String, dynamic> json,
      ) {
    final dynamic checkItemsJson =
    json['checkItems'];

    return WasteGuideDetail(
      id: (json['id'] as num?)?.toInt() ?? 0,
      summary: json['summary'] as String?,
      disposalMethod:
      json['disposalMethod'] as String?,
      caution: json['caution'] as String?,
      checkItems: checkItemsJson is List
          ? checkItemsJson
          .whereType<Map<String, dynamic>>()
          .map(
        WasteGuideCheckItem.fromJson,
      )
          .toList()
          : const [],
    );
  }
}

class WasteDetailCategory {
  const WasteDetailCategory({
    required this.id,
    required this.code,
    required this.name,
  });

  final int id;
  final String code;
  final String name;

  factory WasteDetailCategory.fromJson(
      Map<String, dynamic> json,
      ) {
    return WasteDetailCategory(
      id: (json['id'] as num?)?.toInt() ?? 0,
      code: json['code'] as String? ?? '',
      name: json['name'] as String? ?? '',
    );
  }
}

class ManagedComplexScheduleTime {
  const ManagedComplexScheduleTime({
    required this.scheduleId,
    required this.dayOfWeek,
    required this.startTime,
    required this.endTime,
    required this.alwaysAvailable,
  });

  final int scheduleId;
  final String? dayOfWeek;
  final String? startTime;
  final String? endTime;
  final bool alwaysAvailable;

  factory ManagedComplexScheduleTime.fromJson(
      Map<String, dynamic> json,
      ) {
    return ManagedComplexScheduleTime(
      scheduleId:
      (json['scheduleId'] as num?)?.toInt() ??
          0,
      dayOfWeek:
      json['dayOfWeek'] as String?,
      startTime:
      json['startTime'] as String?,
      endTime:
      json['endTime'] as String?,
      alwaysAvailable:
      json['alwaysAvailable'] as bool? ??
          false,
    );
  }
}

class WasteItemDetail {
  const WasteItemDetail({
    required this.id,
    required this.name,
    required this.imageUrl,
    required this.category,
    required this.guide,
    required this.availableToday,
    required this.availableNow,
    required this.nextAvailableDate,
    required this.nextAvailableAt,
    required this.schedules,
  });

  final int id;
  final String name;
  final String? imageUrl;
  final WasteDetailCategory category;
  final WasteGuideDetail? guide;

  final bool availableToday;
  final bool availableNow;

  final String? nextAvailableDate;
  final String? nextAvailableAt;

  final List<ManagedComplexScheduleTime> schedules;

  factory WasteItemDetail.fromJson(
      Map<String, dynamic> json,
      ) {
    final dynamic categoryJson =
    json['category'];

    final dynamic guideJson =
    json['guide'];

    final dynamic schedulesJson =
    json['schedules'];

    return WasteItemDetail(
      id: (json['id'] as num?)?.toInt() ?? 0,
      name: json['name'] as String? ?? '',
      imageUrl: json['imageUrl'] as String?,
      category: WasteDetailCategory.fromJson(
        categoryJson is Map<String, dynamic>
            ? categoryJson
            : <String, dynamic>{},
      ),
      guide: guideJson is Map<String, dynamic>
          ? WasteGuideDetail.fromJson(
        guideJson,
      )
          : null,
      availableToday:
      json['availableToday'] as bool? ??
          false,
      availableNow:
      json['availableNow'] as bool? ??
          false,
      nextAvailableDate:
      json['nextAvailableDate']
          ?.toString(),
      nextAvailableAt:
      json['nextAvailableAt']
          ?.toString(),
      schedules: schedulesJson is List
          ? schedulesJson
          .whereType<Map<String, dynamic>>()
          .map(
        ManagedComplexScheduleTime
            .fromJson,
      )
          .toList()
          : const [],
    );
  }
}

class GeneralHousingSchedule {
  const GeneralHousingSchedule({
    required this.wasteType,
    required this.wasteTypeLabel,
    required this.collectionAreaMatched,
    required this.collectionAreaName,
    required this.targetAreaName,
    required this.scheduleAvailable,
    required this.emissionDays,
    required this.startTime,
    required this.endTime,
    required this.overnight,
    required this.availableToday,
    required this.availableNow,
    required this.nextAvailableDate,
    required this.nextAvailableAt,
    required this.emissionMethod,
    required this.emissionPlace,
    required this.uncollectedDay,
  });

  final String wasteType;
  final String wasteTypeLabel;

  final bool collectionAreaMatched;
  final String? collectionAreaName;
  final String? targetAreaName;

  final bool scheduleAvailable;

  final String? emissionDays;
  final String? startTime;
  final String? endTime;

  final bool overnight;

  final bool availableToday;
  final bool availableNow;

  final String? nextAvailableDate;
  final String? nextAvailableAt;

  final String? emissionMethod;
  final String? emissionPlace;
  final String? uncollectedDay;

  factory GeneralHousingSchedule.fromJson(
      Map<String, dynamic> json,
      ) {
    return GeneralHousingSchedule(
      wasteType:
      json['wasteType'] as String? ?? '',
      wasteTypeLabel:
      json['wasteTypeLabel'] as String? ?? '',
      collectionAreaMatched:
      json['collectionAreaMatched']
      as bool? ??
          false,
      collectionAreaName:
      json['collectionAreaName']
      as String?,
      targetAreaName:
      json['targetAreaName'] as String?,
      scheduleAvailable:
      json['scheduleAvailable'] as bool? ??
          false,
      emissionDays:
      json['emissionDays'] as String?,
      startTime:
      json['startTime'] as String?,
      endTime:
      json['endTime'] as String?,
      overnight:
      json['overnight'] as bool? ?? false,
      availableToday:
      json['availableToday'] as bool? ??
          false,
      availableNow:
      json['availableNow'] as bool? ??
          false,
      nextAvailableDate:
      json['nextAvailableDate']
          ?.toString(),
      nextAvailableAt:
      json['nextAvailableAt']
          ?.toString(),
      emissionMethod:
      json['emissionMethod'] as String?,
      emissionPlace:
      json['emissionPlace'] as String?,
      uncollectedDay:
      json['uncollectedDay'] as String?,
    );
  }
}

class GeneralHousingScheduleResult {
  const GeneralHousingScheduleResult({
    required this.addressName,
    required this.sido,
    required this.sigungu,
    required this.schedules,
  });

  final String addressName;
  final String sido;
  final String sigungu;

  final List<GeneralHousingSchedule> schedules;

  factory GeneralHousingScheduleResult.fromJson(
      Map<String, dynamic> json,
      ) {
    final dynamic schedulesJson =
    json['schedules'];

    return GeneralHousingScheduleResult(
      addressName:
      json['addressName'] as String? ?? '',
      sido: json['sido'] as String? ?? '',
      sigungu:
      json['sigungu'] as String? ?? '',
      schedules: schedulesJson is List
          ? schedulesJson
          .whereType<Map<String, dynamic>>()
          .map(
        GeneralHousingSchedule.fromJson,
      )
          .toList()
          : const [],
    );
  }
}

class WasteDetailApiException
    implements Exception {
  const WasteDetailApiException(
      this.message, {
        this.unauthorized = false,
      });

  final String message;
  final bool unauthorized;

  @override
  String toString() => message;
}

class WasteDetailApi {
  WasteDetailApi._();

  static Future<WasteItemDetail> getItemDetail(
      int wasteItemId,
      ) async {
    final http.Response response;

    try {
      response =
      await AuthenticatedApiClient.get(
        '/api/waste/items/$wasteItemId',
      );
    } on AuthenticatedApiException catch (
    exception
    ) {
      throw WasteDetailApiException(
        exception.message,
        unauthorized:
        exception.unauthorized,
      );
    }

    final body =
    _decodeBody(response);

    _validateResponse(
      response,
      body,
    );

    final dynamic data = body['data'];

    if (data is! Map<String, dynamic>) {
      throw const WasteDetailApiException(
        '품목 상세 정보가 없습니다.',
      );
    }

    return WasteItemDetail.fromJson(
      data,
    );
  }

  static Future<GeneralHousingScheduleResult>
  getGeneralHousingSchedule() async {
    final http.Response response;

    try {
      response =
      await AuthenticatedApiClient.get(
        '/api/schedules/me/general-housing',
      );
    } on AuthenticatedApiException catch (
    exception
    ) {
      throw WasteDetailApiException(
        exception.message,
        unauthorized:
        exception.unauthorized,
      );
    }

    final body =
    _decodeBody(response);

    _validateResponse(
      response,
      body,
    );

    final dynamic data = body['data'];

    if (data is! Map<String, dynamic>) {
      throw const WasteDetailApiException(
        '지역 배출 일정 정보가 없습니다.',
      );
    }

    return GeneralHousingScheduleResult
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

  static void _validateResponse(
      http.Response response,
      Map<String, dynamic> body,
      ) {
    if (response.statusCode == 401) {
      throw const WasteDetailApiException(
        '로그인이 만료되었습니다.',
        unauthorized: true,
      );
    }

    if (response.statusCode < 200 ||
        response.statusCode >= 300 ||
        body['success'] != true) {
      throw WasteDetailApiException(
        body['message'] as String? ??
            '정보를 불러오지 못했습니다.',
      );
    }
  }
}