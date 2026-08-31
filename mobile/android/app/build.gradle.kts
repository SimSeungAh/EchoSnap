plugins {
    id("com.android.application")

    // Flutter Gradle Plugin은
    // Android / Kotlin 플러그인 이후에 적용합니다.
    id("dev.flutter.flutter-gradle-plugin")
}

android {
    namespace = "com.smartrecycle.smart_recycle"

    compileSdk = flutter.compileSdkVersion

    ndkVersion = flutter.ndkVersion

    compileOptions {
        sourceCompatibility =
            JavaVersion.VERSION_17

        targetCompatibility =
            JavaVersion.VERSION_17
    }

    defaultConfig {
        applicationId =
            "com.smartrecycle.smart_recycle"

        /*
         * tflite_flutter 0.12.x 실기기 사용 기준에 맞춰
         * Android API 26 이상으로 설정합니다.
         */
        minSdk = 26

        targetSdk =
            flutter.targetSdkVersion

        versionCode =
            flutter.versionCode

        versionName =
            flutter.versionName
    }

    buildTypes {
        release {
            /*
             * 현재 개발 단계에서는
             * debug signing key로 release 실행을 허용합니다.
             */
            signingConfig =
                signingConfigs
                    .getByName("debug")
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget =
            org.jetbrains.kotlin.gradle.dsl
                .JvmTarget
                .JVM_17
    }
}

flutter {
    source = "../.."
}