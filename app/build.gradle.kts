import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// ---------------------------------------------------------------------------
// 版本号
//   CI（打 tag 时）通过 -Pcornered.versionName=1.2.3 -Pcornered.versionCode=10203 注入；
//   本地构建没传参时，用下面的默认值 + 由 versionName 推出的 versionCode。
// versionCode 必须单调递增，否则应用内的「检查更新」和覆盖安装都会失败。
// ---------------------------------------------------------------------------
val fallbackVersionName = "0.2.0"

fun encodeVersionCode(name: String): Int {
    val parts = name.removePrefix("v").split('.')
        .map { seg -> seg.takeWhile { it.isDigit() }.toIntOrNull() ?: 0 }
    val major = parts.getOrElse(0) { 0 }.coerceIn(0, 9_999)
    val minor = parts.getOrElse(1) { 0 }.coerceIn(0, 99)
    val patch = parts.getOrElse(2) { 0 }.coerceIn(0, 99)
    return major * 10_000 + minor * 100 + patch
}

val appVersionName: String = (findProperty("cornered.versionName") as String?)?.trim()
    ?.takeIf { it.isNotEmpty() } ?: fallbackVersionName
val appVersionCode: Int = (findProperty("cornered.versionCode") as String?)?.trim()?.toIntOrNull()
    ?: encodeVersionCode(appVersionName)

// ---------------------------------------------------------------------------
// 签名：优先 keystore.properties（本地，已 gitignore），其次环境变量（CI secrets）。
// 两者都没有时退回 debug 签名，仅够自测；debug 签名每次全新，无法覆盖安装。
// ---------------------------------------------------------------------------
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun signingValue(propKey: String, envKey: String): String? =
    keystoreProperties.getProperty(propKey)?.takeIf { it.isNotBlank() }
        ?: System.getenv(envKey)?.takeIf { it.isNotBlank() }

val releaseStoreFile = signingValue("storeFile", "CORNERED_KEYSTORE")
    ?.let { rootProject.file(it) }
    ?.takeIf { it.exists() }

if (releaseStoreFile == null) {
    // 英文提示：Windows 控制台用 GBK 解码，中文在这里会乱码
    logger.lifecycle(
        "[Cornered] No release keystore found (keystore.properties or CORNERED_KEYSTORE env). " +
            "Release build falls back to debug signing: OK for local play, NOT for publishing."
    )
}

android {
    namespace = "dev.duefor.cornered"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.duefor.cornered"
        minSdk = 26
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersionName

        // 应用内更新读取的仓库与渠道信息
        buildConfigField("String", "UPDATE_REPO", "\"Duefor/Cornered\"")
        buildConfigField("String", "UPDATE_TAG_PREFIX", "\"v\"")
    }

    signingConfigs {
        if (releaseStoreFile != null) {
            create("release") {
                storeFile = releaseStoreFile
                storePassword = signingValue("storePassword", "CORNERED_KEYSTORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "CORNERED_KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "CORNERED_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            // 先关闭混淆，保证第一版稳定出包；APK 体积可接受时不必急着开。
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = if (releaseStoreFile != null) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    lint {
        abortOnError = false
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.02"))
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    testImplementation("junit:junit:4.13.2")
    // 单元测试里用真实的 org.json 实现（android.jar 里的是空壳）
    testImplementation("org.json:json:20231013")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
