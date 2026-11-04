import java.util.Properties

/**
 * Release signing with your upload key. The key and its passwords live in
 * keystore.properties next to settings.gradle.kts - never in git (see
 * docs/play/README.md). Without that file, release builds fall back to the
 * debug key: fine for trying them out, refused by Google Play.
 */
val uploadKey = rootProject.file("keystore.properties").takeIf { it.exists() }?.let { file ->
    Properties().apply { file.inputStream().use { load(it) } }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "de.keinbiervorvier.wear"
    compileSdk {
        version = release(36) { minorApiLevel = 1 }
    }

    defaultConfig {
        // Same package as the phone app (one store listing); its own version codes.
        applicationId = "de.irgendwoistimmervier"
        minSdk = 30
        targetSdk = 36
        versionCode = 2
        versionName = "0.1.0"
    }

    signingConfigs {
        if (uploadKey != null) {
            create("upload") {
                storeFile = rootProject.file(uploadKey.getProperty("storeFile"))
                storePassword = uploadKey.getProperty("storePassword")
                keyAlias = uploadKey.getProperty("keyAlias")
                keyPassword = uploadKey.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("upload") ?: signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":common"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.wear.compose.material3)
    implementation(libs.wear.compose.foundation)
    implementation(libs.watchface.complications.data.source.ktx)
}
