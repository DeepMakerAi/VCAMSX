plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.wangyiheng.vcamsx"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.wangyiheng.vcamsx"
        minSdk = 24
        //noinspection EditedTargetSdkVersion
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
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.3"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

// Core library for Kotlin extensions and utilities
    implementation("androidx.core:core-ktx:1.9.0")

// Lifecycle components for using ViewModel and LiveData in a Kotlin-friendly way
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")

// Compose support for Activities
    implementation("androidx.activity:activity-compose:1.8.0")

// Bill of Materials (BOM) for all Compose libraries, ensures compatible versions
    implementation(platform("androidx.compose:compose-bom:2023.03.00"))

// Compose UI framework
    implementation("androidx.compose.ui:ui")

// Compose library for graphics
    implementation("androidx.compose.ui:ui-graphics")

// Tooling for UI preview in Compose
    implementation("androidx.compose.ui:ui-tooling-preview")

// Material3 design components for Compose
    implementation("androidx.compose.material3:material3")

// Media3 ExoPlayer for handling media playback
    implementation("androidx.media3:media3-exoplayer:1.2.0")
    implementation("androidx.media3:media3-ui:1.2.0")

// JUnit for unit testing
    testImplementation("junit:junit:4.13.2")

// AndroidX Test library for Android-specific JUnit4 helpers
    androidTestImplementation("androidx.test.ext:junit:1.1.5")

// Espresso for UI testing
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

// BOM for Compose in android tests
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.03.00"))

// Compose testing library for JUnit4
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

// Tooling for debugging Compose UIs
    debugImplementation("androidx.compose.ui:ui-tooling")

// Manifest for testing Compose UIs
    debugImplementation("androidx.compose.ui:ui-test-manifest")

// Koin core module for Dependency Injection
    implementation ("io.insert-koin:koin-core:3.2.2")

// Koin module for Android
    implementation ("io.insert-koin:koin-android:3.2.2")

// Koin module for AndroidX Compose
    implementation ("io.insert-koin:koin-androidx-compose:3.2.2")

// Xposed API for advanced customization and hooking into Android apps (compile only)
    compileOnly("de.robv.android.xposed:api:82")


    implementation ("com.crossbowffs.remotepreferences:remotepreferences:0.8")

    implementation ("com.google.code.gson:gson:2.8.8")
}