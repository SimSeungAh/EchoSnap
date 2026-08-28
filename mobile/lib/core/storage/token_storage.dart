import 'package:flutter_secure_storage/flutter_secure_storage.dart';

class TokenStorage {
  TokenStorage._();

  static const FlutterSecureStorage _storage =
  FlutterSecureStorage();

  static const String _accessTokenKey =
      'smart_recycle_access_token';

  static const String _refreshTokenKey =
      'smart_recycle_refresh_token';

  static Future<void> saveTokens({
    required String accessToken,
    required String refreshToken,
  }) async {
    await Future.wait([
      _storage.write(
        key: _accessTokenKey,
        value: accessToken,
      ),
      _storage.write(
        key: _refreshTokenKey,
        value: refreshToken,
      ),
    ]);
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
    final accessToken = await getAccessToken();

    return accessToken != null &&
        accessToken.isNotEmpty;
  }

  static Future<void> clearTokens() async {
    await Future.wait([
      _storage.delete(
        key: _accessTokenKey,
      ),
      _storage.delete(
        key: _refreshTokenKey,
      ),
    ]);
  }
}