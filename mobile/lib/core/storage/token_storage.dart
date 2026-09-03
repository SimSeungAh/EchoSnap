import 'package:flutter_secure_storage/flutter_secure_storage.dart';

class TokenStorage {
  TokenStorage._();

  static const FlutterSecureStorage _storage =
  FlutterSecureStorage();

  static const String _accessTokenKey =
      'echosnap_access_token';

  static const String _refreshTokenKey =
      'echosnap_refresh_token';

  static Future<void> saveTokens({
    required String accessToken,
    required String refreshToken,
  }) async {
    /*
     * 인증 토큰은 병렬로 저장하지 않고
     * 순서대로 저장합니다.
     *
     * 특히 Flutter Web에서는 로그인 직후
     * 사용자 정보 API가 바로 토큰을 다시 읽기 때문에
     * 저장 완료 시점을 명확하게 보장하는 편이 안전합니다.
     */

    await _storage.write(
      key: _refreshTokenKey,
      value: refreshToken,
    );

    await _storage.write(
      key: _accessTokenKey,
      value: accessToken,
    );
  }

  static Future<String?> getAccessToken() {
    return _storage.read(
      key: _accessTokenKey,
    );
  }

  static Future<String?> getRefreshToken() {
    return _storage.read(
      key: _refreshTokenKey,
    );
  }

  static Future<bool> hasAccessToken() async {
    final String? accessToken =
    await getAccessToken();

    return accessToken != null &&
        accessToken.isNotEmpty;
  }

  static Future<void> clearTokens() async {
    /*
     * 삭제도 병렬 처리하지 않습니다.
     *
     * 로그인/로그아웃 상태를 다루는 데이터이므로
     * 단순한 성능보다 일관성을 우선합니다.
     */

    await _storage.delete(
      key: _accessTokenKey,
    );

    await _storage.delete(
      key: _refreshTokenKey,
    );
  }
}