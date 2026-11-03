plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// release 서명은 두 환경 변수가 모두 있을 때만(Actions에서만 — 계획 9절). 값은 출력하지 않음.
val releaseKeystoreFile: String? = providers.environmentVariable("ISSUERANK_KEYSTORE_FILE").orNull
val releaseKeystorePassword: String? = providers.environmentVariable("ISSUERANK_KEYSTORE_PASSWORD").orNull
val hasReleaseKey = !releaseKeystoreFile.isNullOrEmpty() && !releaseKeystorePassword.isNullOrEmpty()

android {
    namespace = "io.github.jwavascript.issuerank"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.jwavascript.issuerank"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseKey) {
            create("release") {
                storeFile = file(releaseKeystoreFile!!)
                storePassword = releaseKeystorePassword
                keyAlias = "issuerank"
                keyPassword = releaseKeystorePassword // PKCS12라 키 비밀번호 = 저장소 비밀번호
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            // 가짜 서버(adb reverse) 기본값 — debug 도구에서 바꿈
            buildConfigField("String", "SERVER_URL", "\"http://127.0.0.1:3100\"")
        }
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            buildConfigField("String", "SERVER_URL", "\"https://issuerank-kappa.vercel.app\"")
            if (hasReleaseKey) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    buildFeatures {
        compose = true
        // SERVER_URL · VERSION_NAME(User-Agent issuerank-android/{versionName})
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    testImplementation(libs.junit)
    testImplementation(libs.org.json)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
