plugins {
    // Các plugin cho ứng dụng Android, Kotlin và Compose
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    // Kapt để xử lý chú thích (Room, Hilt)
    alias(libs.plugins.kotlin.kapt)
    // Hilt để quản lý Dependency Injection
    alias(libs.plugins.hilt.android)
}

android {
    namespace = "com.T2V.simple_expense_tracker"

    compileSdk = 34

    defaultConfig {
        applicationId = "com.T2V.simple_expense_tracker"
        // minSdk = 26 đảm bảo hỗ trợ đa số thiết bị
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }

    kapt {
        correctErrorTypes = true
    }
    // Cấu hình Compose với Kotlin 1.9.23 và Compose Compiler 1.5.11
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Room - Cơ sở dữ liệu cục bộ để lưu trữ giao dịch và quy tắc
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    kapt(libs.room.compiler)

    // Hilt - Dependency Injection, giúp việc quản lý các đối tượng dễ dàng và có thể kiểm thử
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // DataStore - Lưu trữ preferences (theme setting)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Navigation Compose và WorkManager - Điều hướng giữa các màn hình và xử lý tác vụ nền
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.work.runtime.ktx)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}