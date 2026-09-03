import 'dart:convert';

import 'package:http/http.dart' as http;
import 'package:echosnap/core/network/authenticated_api_client.dart';

class ScheduleExceptionInfo {
  const ScheduleExceptionInfo({
    required this.effectiveDate,
    required this.unavailable,
    required this.startTime,
    required this.endTime,
    required this.alwaysAvailable,
    required this.reason,
  });

  final String? effectiveDate;

  final bool unavailable;

  final String? startTime;
  final String? endTime;

  final bool? alwaysAvailable;

  final String? reason;

  factory ScheduleExceptionInfo.fromJson(Map<String, dynamic> json) {
    return ScheduleExceptionInfo(
      effectiveDate: json['effectiveDate']?.toString(),
      unavailable: json['unavailable'] as bool? ?? false,
      startTime: json['startTime']?.toString(),
      endTime: json['endTime']?.toString(),
      alwaysAvailable: json['alwaysAvailable'] as bool?,
      reason: json['reason'] as String?,
    );
  }
}

class GeneralHousingScheduleItem {
  const GeneralHousingScheduleItem({
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
    required this.emissionPlaceType,
    required this.uncollectedDay,
    required this.todayException,
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
  final String? emissionPlaceType;
  final String? uncollectedDay;

  final ScheduleExceptionInfo? todayException;

  factory GeneralHousingScheduleItem.fromJson(Map<String, dynamic> json) {
    final dynamic exceptionJson = json['todayException'];

    return GeneralHousingScheduleItem(
      wasteType: json['wasteType'] as String? ?? '',
      wasteTypeLabel: json['wasteTypeLabel'] as String? ?? '배출 품목',
      collectionAreaMatched: json['collectionAreaMatched'] as bool? ?? false,
      collectionAreaName: json['collectionAreaName'] as String?,
      targetAreaName: json['targetAreaName'] as String?,
      scheduleAvailable: json['scheduleAvailable'] as bool? ?? false,
      emissionDays: json['emissionDays'] as String?,
      startTime: json['startTime']?.toString(),
      endTime: json['endTime']?.toString(),
      overnight: json['overnight'] as bool? ?? false,
      availableToday: json['availableToday'] as bool? ?? false,
      availableNow: json['availableNow'] as bool? ?? false,
      nextAvailableDate: json['nextAvailableDate']?.toString(),
      nextAvailableAt: json['nextAvailableAt']?.toString(),
      emissionMethod: json['emissionMethod'] as String?,
      emissionPlace: json['emissionPlace'] as String?,
      emissionPlaceType: json['emissionPlaceType'] as String?,
      uncollectedDay: json['uncollectedDay'] as String?,
      todayException: exceptionJson is Map<String, dynamic>
          ? ScheduleExceptionInfo.fromJson(exceptionJson)
          : null,
    );
  }
}

class GeneralHousingScheduleData {
  const GeneralHousingScheduleData({
    required this.addressName,
    required this.sido,
    required this.sigungu,
    required this.administrativeDong,
    required this.legalDong,
    required this.referenceDateTime,
    required this.schedules,
  });

  final String addressName;

  final String sido;
  final String sigungu;

  final String? administrativeDong;
  final String? legalDong;

  final String? referenceDateTime;

  final List<GeneralHousingScheduleItem> schedules;

  factory GeneralHousingScheduleData.fromJson(Map<String, dynamic> json) {
    final dynamic schedulesJson = json['schedules'];

    return GeneralHousingScheduleData(
      addressName: json['addressName'] as String? ?? '',
      sido: json['sido'] as String? ?? '',
      sigungu: json['sigungu'] as String? ?? '',
      administrativeDong: json['administrativeDong'] as String?,
      legalDong: json['legalDong'] as String?,
      referenceDateTime: json['referenceDateTime']?.toString(),
      schedules: schedulesJson is List
          ? schedulesJson
                .whereType<Map<String, dynamic>>()
                .map(GeneralHousingScheduleItem.fromJson)
                .toList()
          : const [],
    );
  }
}

class ManagedScheduleTime {
  const ManagedScheduleTime({
    required this.scheduleId,
    required this.dayOfWeek,
    required this.startTime,
    required this.endTime,
    required this.alwaysAvailable,
  });

  final String? dayOfWeek;
  final int scheduleId;

  final String? startTime;
  final String? endTime;

  final bool alwaysAvailable;

  factory ManagedScheduleTime.fromJson(Map<String, dynamic> json) {
    return ManagedScheduleTime(
      scheduleId: (json['scheduleId'] as num?)?.toInt() ?? 0,
      dayOfWeek: json['dayOfWeek']?.toString(),
      startTime: json['startTime']?.toString(),
      endTime: json['endTime']?.toString(),
      alwaysAvailable: json['alwaysAvailable'] as bool? ?? false,
    );
  }
}

class ManagedScheduleItem {
  const ManagedScheduleItem({
    required this.wasteItemId,
    required this.name,
    required this.categoryName,
    required this.availableToday,
    required this.availableNow,
    required this.nextAvailableDate,
    required this.nextAvailableAt,
    required this.schedules,
    required this.todayException,
  });

  final String name;
  final int wasteItemId;
  final String categoryName;

  final bool availableToday;
  final bool availableNow;

  final String? nextAvailableDate;
  final String? nextAvailableAt;

  final List<ManagedScheduleTime> schedules;

  final ScheduleExceptionInfo? todayException;

  factory ManagedScheduleItem.fromJson(Map<String, dynamic> json) {
    final dynamic wasteItem = json['wasteItem'];

    String name = '배출 품목';
    int wasteItemId = 0;
    String categoryName = '';

    if (wasteItem is Map<String, dynamic>) {
      name = wasteItem['name'] as String? ?? name;
      wasteItemId = (wasteItem['id'] as num?)?.toInt() ?? 0;

      final dynamic category = wasteItem['category'];

      if (category is Map<String, dynamic>) {
        categoryName = category['name'] as String? ?? '';
      }
    }

    final dynamic schedulesJson = json['schedules'];

    final dynamic exceptionJson = json['todayException'];

    return ManagedScheduleItem(
      wasteItemId: wasteItemId,
      name: name,
      categoryName: categoryName,
      availableToday: json['availableToday'] as bool? ?? false,
      availableNow: json['availableNow'] as bool? ?? false,
      nextAvailableDate: json['nextAvailableDate']?.toString(),
      nextAvailableAt: json['nextAvailableAt']?.toString(),
      schedules: schedulesJson is List
          ? schedulesJson
                .whereType<Map<String, dynamic>>()
                .map(ManagedScheduleTime.fromJson)
                .toList()
          : const [],
      todayException: exceptionJson is Map<String, dynamic>
          ? ScheduleExceptionInfo.fromJson(exceptionJson)
          : null,
    );
  }
}

class ManagedComplexScheduleData {
  const ManagedComplexScheduleData({
    required this.apartmentName,
    required this.referenceDateTime,
    required this.items,
  });

  final String apartmentName;

  final String? referenceDateTime;

  final List<ManagedScheduleItem> items;

  factory ManagedComplexScheduleData.fromJson(Map<String, dynamic> json) {
    final dynamic itemsJson = json['items'];

    return ManagedComplexScheduleData(
      apartmentName: json['apartmentName'] as String? ?? '공동주택',
      referenceDateTime: json['referenceDateTime']?.toString(),
      items: itemsJson is List
          ? itemsJson
                .whereType<Map<String, dynamic>>()
                .map(ManagedScheduleItem.fromJson)
                .toList()
          : const [],
    );
  }
}

class ConfirmableScheduleReport {
  const ConfirmableScheduleReport({
    required this.id,
    required this.wasteItemName,
    required this.reporterNickname,
    required this.dayOfWeek,
    required this.startTime,
    required this.endTime,
    required this.alwaysAvailable,
    required this.note,
    required this.confirmedCount,
    required this.differentCount,
    required this.myConfirmationValue,
  });

  final int id;
  final String wasteItemName;
  final String reporterNickname;
  final String? dayOfWeek;
  final String? startTime;
  final String? endTime;
  final bool alwaysAvailable;
  final String? note;
  final int confirmedCount;
  final int differentCount;
  final String? myConfirmationValue;

  factory ConfirmableScheduleReport.fromJson(Map<String, dynamic> json) {
    final report = json['report'] is Map<String, dynamic>
        ? json['report'] as Map<String, dynamic>
        : <String, dynamic>{};
    return ConfirmableScheduleReport(
      id: (report['id'] as num?)?.toInt() ?? 0,
      wasteItemName: report['wasteItemName'] as String? ?? '재활용품',
      reporterNickname: report['reporterNickname'] as String? ?? '이웃 주민',
      dayOfWeek: report['reportedDayOfWeek']?.toString(),
      startTime: report['reportedStartTime']?.toString(),
      endTime: report['reportedEndTime']?.toString(),
      alwaysAvailable: report['reportedAlwaysAvailable'] as bool? ?? false,
      note: report['reportNote'] as String?,
      confirmedCount: (json['confirmedCount'] as num?)?.toInt() ?? 0,
      differentCount: (json['differentCount'] as num?)?.toInt() ?? 0,
      myConfirmationValue: json['myConfirmationValue']?.toString(),
    );
  }
}

class ScheduleApiException implements Exception {
  const ScheduleApiException(this.message, {this.unauthorized = false});

  final String message;
  final bool unauthorized;

  @override
  String toString() => message;
}

class ScheduleApi {
  ScheduleApi._();

  static Future<GeneralHousingScheduleData> getGeneralHousingSchedule() async {
    final Map<String, dynamic> body = await _get(
      '/api/schedules/me/general-housing',
    );

    final dynamic data = body['data'];

    if (data is! Map<String, dynamic>) {
      throw const ScheduleApiException('지역 배출 일정 정보가 없습니다.');
    }

    return GeneralHousingScheduleData.fromJson(data);
  }

  static Future<ManagedComplexScheduleData> getManagedComplexSchedule() async {
    final Map<String, dynamic> body = await _get('/api/schedules/me');

    final dynamic data = body['data'];

    if (data is! Map<String, dynamic>) {
      throw const ScheduleApiException('공동주택 배출 일정 정보가 없습니다.');
    }

    return ManagedComplexScheduleData.fromJson(data);
  }

  static Future<void> reportApartmentSchedule({
    required int wasteItemId,
    required String dayOfWeek,
    required String startTime,
    required String endTime,
    required bool alwaysAvailable,
    int? referenceScheduleId,
    String? note,
  }) async {
    final http.Response response;
    try {
      response = await AuthenticatedApiClient.post(
        '/api/schedule-reports/apartment',
        body: {
          'reportType': referenceScheduleId == null
              ? 'INITIAL_SCHEDULE'
              : 'SCHEDULE_CORRECTION',
          'wasteItemId': wasteItemId,
          'referenceScheduleId': referenceScheduleId,
          'reportedDayOfWeek': alwaysAvailable ? null : dayOfWeek,
          'reportedStartTime': alwaysAvailable ? null : startTime,
          'reportedEndTime': alwaysAvailable ? null : endTime,
          'reportedAlwaysAvailable': alwaysAvailable,
          'reportNote': note,
        },
      );
    } on AuthenticatedApiException catch (exception) {
      throw ScheduleApiException(
        exception.message,
        unauthorized: exception.unauthorized,
      );
    }

    final Map<String, dynamic> body = _decode(response);
    _validate(response, body);
  }

  static Future<List<ConfirmableScheduleReport>> getConfirmableReports() async {
    final body = await _get('/api/schedule-reports/confirmable');
    final data = body['data'];
    if (data is! List) return const [];
    return data
        .whereType<Map<String, dynamic>>()
        .map(ConfirmableScheduleReport.fromJson)
        .toList();
  }

  static Future<void> confirmReport(int reportId, String value) async {
    final http.Response response;
    try {
      response = await AuthenticatedApiClient.put(
        '/api/schedule-reports/$reportId/confirmation',
        body: {'value': value},
      );
    } on AuthenticatedApiException catch (exception) {
      throw ScheduleApiException(
        exception.message,
        unauthorized: exception.unauthorized,
      );
    }
    final body = _decode(response);
    _validate(response, body);
  }

  static Future<Map<String, dynamic>> _get(String path) async {
    final http.Response response;

    try {
      response = await AuthenticatedApiClient.get(path);
    } on AuthenticatedApiException catch (exception) {
      throw ScheduleApiException(
        exception.message,
        unauthorized: exception.unauthorized,
      );
    }

    Map<String, dynamic> body;

    try {
      final dynamic decoded = jsonDecode(utf8.decode(response.bodyBytes));

      if (decoded is! Map<String, dynamic>) {
        throw const FormatException();
      }

      body = decoded;
    } catch (_) {
      throw ScheduleApiException(
        '배출 일정 응답을 처리할 수 없습니다. '
        '(HTTP ${response.statusCode})',
      );
    }

    if (response.statusCode == 401) {
      throw const ScheduleApiException('로그인이 만료되었습니다.', unauthorized: true);
    }

    if (response.statusCode < 200 ||
        response.statusCode >= 300 ||
        body['success'] != true) {
      throw ScheduleApiException(
        body['message'] as String? ?? '배출 일정을 불러오지 못했습니다.',
      );
    }

    return body;
  }

  static Map<String, dynamic> _decode(http.Response response) {
    try {
      final decoded = jsonDecode(utf8.decode(response.bodyBytes));
      if (decoded is Map<String, dynamic>) return decoded;
    } catch (_) {}
    throw ScheduleApiException(
      '서버 응답을 처리할 수 없습니다. (HTTP ${response.statusCode})',
    );
  }

  static void _validate(http.Response response, Map<String, dynamic> body) {
    if (response.statusCode == 401) {
      throw const ScheduleApiException('로그인이 만료되었습니다.', unauthorized: true);
    }
    if (response.statusCode < 200 ||
        response.statusCode >= 300 ||
        body['success'] != true) {
      throw ScheduleApiException(
        body['message'] as String? ?? '요청을 처리하지 못했습니다.',
      );
    }
  }
}
