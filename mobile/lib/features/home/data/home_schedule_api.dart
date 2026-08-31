import 'dart:convert';

import 'package:http/http.dart' as http;
import 'package:smart_recycle/core/network/authenticated_api_client.dart';

class HomeScheduleEntry {
  const HomeScheduleEntry({
    required this.label,
    required this.availableToday,
    required this.availableNow,
    this.nextAvailableDate,
    this.nextAvailableAt,
  });

  final String label;

  final bool availableToday;
  final bool availableNow;

  final String? nextAvailableDate;
  final String? nextAvailableAt;

  DateTime? get nextDateTime {
    final at = nextAvailableAt;

    if (at != null) {
      final parsed = DateTime.tryParse(at);

      if (parsed != null) {
        return parsed;
      }
    }

    final date = nextAvailableDate;

    if (date != null) {
      return DateTime.tryParse(date);
    }

    return null;
  }
}

class HomeScheduleSnapshot {
  const HomeScheduleSnapshot({
    required this.locationName,
    required this.entries,
  });

  final String locationName;
  final List<HomeScheduleEntry> entries;

  List<HomeScheduleEntry> get availableNowEntries {
    return entries
        .where(
          (entry) => entry.availableNow,
    )
        .toList();
  }

  List<HomeScheduleEntry>
  get availableTodayEntries {
    return entries
        .where(
          (entry) => entry.availableToday,
    )
        .toList();
  }

  HomeScheduleEntry? get nearestNextEntry {
    final candidates =
    entries
        .where(
          (entry) =>
      entry.nextDateTime != null,
    )
        .toList();

    if (candidates.isEmpty) {
      return null;
    }

    candidates.sort(
          (a, b) => a.nextDateTime!.compareTo(
        b.nextDateTime!,
      ),
    );

    return candidates.first;
  }
}

class HomeScheduleApiException
    implements Exception {
  const HomeScheduleApiException(
      this.message, {
        this.unauthorized = false,
      });

  final String message;
  final bool unauthorized;

  @override
  String toString() => message;
}

class HomeScheduleApi {
  HomeScheduleApi._();

  static Future<HomeScheduleSnapshot>
  getSchedule({
    required String residenceType,
  }) async {
    if (residenceType ==
        'GENERAL_HOUSING') {
      return _getGeneralHousingSchedule();
    }

    if (residenceType ==
        'MANAGED_COMPLEX') {
      return _getManagedComplexSchedule();
    }

    return const HomeScheduleSnapshot(
      locationName: '',
      entries: [],
    );
  }

  static Future<HomeScheduleSnapshot>
  _getGeneralHousingSchedule() async {
    final body = await _get(
      '/api/schedules/me/general-housing',
    );

    final dynamic data = body['data'];

    if (data is! Map<String, dynamic>) {
      throw const HomeScheduleApiException(
        '지역 배출 일정 정보가 없습니다.',
      );
    }

    final dynamic schedules =
    data['schedules'];

    final List<HomeScheduleEntry> entries =
    schedules is List
        ? schedules
        .whereType<
        Map<String, dynamic>>()
        .map(
          (schedule) {
        return HomeScheduleEntry(
          label: schedule[
          'wasteTypeLabel']
          as String? ??
              '배출 품목',
          availableToday:
          schedule[
          'availableToday']
          as bool? ??
              false,
          availableNow:
          schedule[
          'availableNow']
          as bool? ??
              false,
          nextAvailableDate:
          schedule[
          'nextAvailableDate']
              ?.toString(),
          nextAvailableAt:
          schedule[
          'nextAvailableAt']
              ?.toString(),
        );
      },
    )
        .toList()
        : <HomeScheduleEntry>[];

    return HomeScheduleSnapshot(
      locationName:
      data['addressName'] as String? ??
          '',
      entries: entries,
    );
  }

  static Future<HomeScheduleSnapshot>
  _getManagedComplexSchedule() async {
    final body = await _get(
      '/api/schedules/me',
    );

    final dynamic data = body['data'];

    if (data is! Map<String, dynamic>) {
      throw const HomeScheduleApiException(
        '공동주택 배출 일정 정보가 없습니다.',
      );
    }

    final dynamic items = data['items'];

    final List<HomeScheduleEntry> entries =
    items is List
        ? items
        .whereType<
        Map<String, dynamic>>()
        .map(
          (item) {
        final dynamic wasteItem =
        item['wasteItem'];

        String label =
            '배출 품목';

        if (wasteItem
        is Map<String, dynamic>) {
          label =
              wasteItem['name']
              as String? ??
                  label;
        }

        return HomeScheduleEntry(
          label: label,
          availableToday:
          item['availableToday']
          as bool? ??
              false,
          availableNow:
          item['availableNow']
          as bool? ??
              false,
          nextAvailableDate:
          item['nextAvailableDate']
              ?.toString(),
          nextAvailableAt:
          item['nextAvailableAt']
              ?.toString(),
        );
      },
    )
        .toList()
        : <HomeScheduleEntry>[];

    return HomeScheduleSnapshot(
      locationName:
      data['apartmentName']
      as String? ??
          '',
      entries: entries,
    );
  }

  static Future<Map<String, dynamic>>
  _get(
      String path,
      ) async {
    final http.Response response;

    try {
      response =
      await AuthenticatedApiClient.get(
        path,
      );
    } on AuthenticatedApiException catch (
    exception
    ) {
      throw HomeScheduleApiException(
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
      throw HomeScheduleApiException(
        '배출 일정 응답을 처리할 수 없습니다. '
            '(HTTP ${response.statusCode})',
      );
    }

    if (response.statusCode == 401) {
      throw const HomeScheduleApiException(
        '로그인이 만료되었습니다.',
        unauthorized: true,
      );
    }

    if (response.statusCode < 200 ||
        response.statusCode >= 300 ||
        body['success'] != true) {
      throw HomeScheduleApiException(
        body['message'] as String? ??
            '배출 일정을 불러오지 못했습니다.',
      );
    }

    return body;
  }
}