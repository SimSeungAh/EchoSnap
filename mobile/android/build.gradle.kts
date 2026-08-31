import com.android.build.gradle.BaseExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

val newBuildDir: Directory =
    rootProject.layout.buildDirectory
        .dir("../../build")
        .get()

rootProject.layout.buildDirectory
    .value(newBuildDir)

/*
 * Flutter 3.44+ / AGP 9 환경에서 일부 플러그인의
 * Java/Kotlin JVM target이 서로 다르게 잡히는 문제를 방지합니다.
 *
 * 현재 tflite_flutter 0.12.1:
 * Java target = 11
 *
 * SmartRecycle / Kotlin:
 * JVM target = 17
 *
 * 따라서 모든 Android 서브프로젝트를 JVM 17로 통일합니다.
 */
subprojects {
    val newSubprojectBuildDir: Directory =
        newBuildDir.dir(project.name)

    project.layout.buildDirectory
        .value(newSubprojectBuildDir)

    afterEvaluate {
        extensions
            .findByType(BaseExtension::class.java)
            ?.apply {
                compileOptions {
                    sourceCompatibility =
                        JavaVersion.VERSION_17

                    targetCompatibility =
                        JavaVersion.VERSION_17
                }
            }
    }

    tasks
        .withType<KotlinCompile>()
        .configureEach {
            compilerOptions {
                jvmTarget.set(
                    JvmTarget.JVM_17
                )
            }
        }
}

/*
 * 위 JVM 설정 블록보다 뒤에 있어야 합니다.
 *
 * evaluationDependsOn이 먼저 실행되면
 * 일부 플러그인이 이미 평가된 뒤 afterEvaluate를
 * 등록하려고 하면서 오류가 날 수 있습니다.
 */
subprojects {
    project.evaluationDependsOn(":app")
}

tasks.register<Delete>("clean") {
    delete(
        rootProject.layout.buildDirectory
    )
}