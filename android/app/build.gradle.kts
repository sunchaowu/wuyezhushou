import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

val appVersionProps = Properties().apply {
    val versionFile = rootProject.file("version.properties")
    if (versionFile.exists()) {
        versionFile.inputStream().use { load(it) }
    }
}
val appVersionCode = appVersionProps.getProperty("versionCode", "1").toInt()
val appVersionName = listOf(
    appVersionProps.getProperty("versionMajor", "1"),
    appVersionProps.getProperty("versionMinor", "0"),
    appVersionProps.getProperty("versionPatch", "0"),
).joinToString(".")

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

fun signingProperty(name: String): String? =
    System.getenv(name)?.takeIf { it.isNotBlank() }
        ?: keystoreProperties.getProperty(
            when (name) {
                "KEYSTORE_FILE" -> "storeFile"
                "KEYSTORE_PASSWORD" -> "storePassword"
                "KEY_ALIAS" -> "keyAlias"
                "KEY_PASSWORD" -> "keyPassword"
                else -> name
            },
        )?.takeIf { it.isNotBlank() && !it.startsWith("请填写") }

val releaseStoreFilePath = signingProperty("KEYSTORE_FILE")
val releaseStorePassword = signingProperty("KEYSTORE_PASSWORD")
val releaseKeyAlias = signingProperty("KEY_ALIAS")
val releaseKeyPassword = signingProperty("KEY_PASSWORD")
val hasReleaseSigning = listOf(
    releaseStoreFilePath,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { it != null }

android {
    namespace = "com.guanjiazhushou.assistant"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.guanjiazhushou.assistant"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = appVersionCode
        versionName = appVersionName

        buildConfigField("int", "MIN_SDK", libs.versions.minSdk.get())
        buildConfigField("int", "TARGET_SDK", libs.versions.targetSdk.get())
        buildConfigField("int", "COMPILE_SDK", libs.versions.compileSdk.get())
        buildConfigField("String", "KOTLIN_VERSION", "\"${libs.versions.kotlin.get()}\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            // First ship arm64; expand ABIs only after device matrix validates
            abiFilters += listOf("arm64-v8a")
        }
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFilePath!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            // Keep uncompressed .so packaging consistent with sherpa-onnx AAR
            useLegacyPackaging = true
            // microsoft-onnxruntime-android and sherpa both ship libonnxruntime.so
            pickFirsts += listOf("**/libonnxruntime.so")
        }
    }

    androidResources {
        // Keep ONNX models uncompressed for AssetManager mmap / sherpa load
        noCompress += listOf("onnx")
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.compose.charts)
    implementation(libs.okhttp)
    implementation(libs.nanohttpd)
    // On-device Tiny NLU (approved listen-assistant plan M3.5)
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.17.3")
    // Offline streaming ASR (sherpa-onnx Zipformer-zh); models live in assets/asr/
    // Prefer local AAR (scripts/fetch-sherpa-aar.sh) to avoid flaky JitPack transfers.
    val sherpaAar = file("libs/sherpa-onnx-v1.13.5.aar")
    if (sherpaAar.exists()) {
        implementation(files(sherpaAar))
    } else {
        implementation("com.github.k2-fsa.sherpa-onnx:sherpa-onnx:v1.13.5")
    }

    testImplementation("junit:junit:4.13.2")
}
