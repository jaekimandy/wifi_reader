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

    // Zetic MLange for on-device AI
    // TODO: Add actual Zetic MLange dependency when available
    // Check ZETIC_MLange_apps repository for exact dependency
    // implementation("com.zetic:mlange:1.0.0")

    // ML/AI dependencies - TensorFlow Lite and Tesseract OCR (testing 2.16.1 before LiteRT migration)
    implementation("org.tensorflow:tensorflow-lite:2.16.1")
    implementation("org.tensorflow:tensorflow-lite-gpu:2.16.1")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")

    // Tesseract OCR for Android
    implementation("com.rmtheis:tess-two:9.1.0")

    // TODO: Add TrOCR TFLite models to assets folder when ready:
    // - trocr_text_detection.tflite (text detection)
    // - trocr_text_recognition.tflite (text recognition)

    // Image processing - OpenCV (local module)
    implementation(project(":opencv"))

    // Note: Removed Google ML Kit - now using EasyOCR with TensorFlow Lite
    // implementation("com.google.mlkit:text-recognition:16.0.0")

    // JSON parsing
    implementation("com.google.code.gson:gson:2.10.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}