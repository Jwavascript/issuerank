// Kotlin은 AGP 9 내장 Kotlin(org.jetbrains.kotlin.android 안 씀).
// AGP 9.5.0은 KGP 2.2.10에 기대지만 같은 classpath의 Compose 컴파일러 플러그인 2.4.21이 KGP 2.4.21을 끌어와
// Gradle이 높은 판을 고른다. 확인: ./gradlew buildEnvironment → kotlin-gradle-plugin:2.2.10 -> 2.4.21
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
