plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.zetic.wifireader"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.zetic.wifireader"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // BuildConfig fields for API keys (similar to whisper app pattern)
        buildConfigField("String", "DETECT_API_KEY", "\"dev_9ba20e80c3fd4edf80f94906aa0ae27d\"")
        buildConfigField("String", "RECOG_API_KEY", "\"dev_7c93c5d85a2a4ec399f86ac1c2ca1f17\"")
        buildConfigField("String", "FACE_DEBUG_KEY", "\"dev_854ee24efea74a05852a50916e61518f\"")
        buildConfigField("String", "OLD_API_KEY", "\"dev_fc0e72ca87df47308bf88063c8f69f32\"")

        // Model keys - Using Zetic MLange for all models
        buildConfigField("String", "TEXT_DETECT_MODEL", "\"jkim711/text_detect2\"")
        buildConfigField("String", "TEXT_RECOG_MODEL", "\"jkim711/text_recog3\"")
        buildConfigField("String", "YOLO_MODEL", "\"Ultralytics/YOLOv8n\"")
        buildConfigField("String", "FACE_MODEL", "\"deepinsight/scrfd_500m\"")
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-ktx:1.8.2")

    // Camera dependencies
    implementation("androidx.camera:camera-core:1.3.1")
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-video:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")
    implementation("androidx.camera:camera-extensions:1.3.1")

    // Permissions
    implementation("androidx.activity:activity-compose:1.8.2")

    // Zetic MLange for on-device AI - handles all model inference
    implementation("com.zeticai.mlange:mlange:1.3.0")

    // JSON parsing
    implementation("com.google.code.gson:gson:2.10.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}