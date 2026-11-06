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
    namespace = "de.keinbiervorvier.phone"
    compileSdk {
        version = release(36) { minorApiLevel = 1 }
    }

    defaultConfig {
        // Phone and watch share one package, so they share one store listing.
        applicationId = "de.irgendwoistimmervier"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "0.1.1"
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

    bundle {
        // Ship German and English to everyone. With language splits, Play installs only
        // the device language, and switching it later left the app in the old language.
        language { enableSplit = false }
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
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)
}
