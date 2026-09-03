class AppConfig {
  AppConfig._();

  static const String apiBaseUrl = String.fromEnvironment(
    'API_BASE_URL',
    // USB 개발 시 `adb reverse tcp:8080 tcp:8080`으로 기기와 PC를 연결합니다.
    // Wi-Fi 실행 시에는 --dart-define=API_BASE_URL=http://<PC-IP>:8080 을 사용합니다.
    defaultValue: 'http://localhost:8080',
  );
}
