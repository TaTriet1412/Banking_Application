plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.bankingapplication"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.bankingapplication"
        minSdk = 24
        targetSdk = 35
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:33.4.0"))
    // Thêm các phụ thuộc các dịch vụ cụ thể của Firebase
    implementation("com.google.firebase:firebase-auth") // Firebase Authentication
    implementation("com.google.firebase:firebase-storage") // Firebase Storage
    implementation("com.google.firebase:firebase-firestore") // Firebase Firestore
    implementation("com.google.android.gms:play-services-vision:20.0.0")
    implementation("com.google.mlkit:text-recognition:16.0.0-beta5")
    implementation("com.github.dikamahard:FaceCompareLibrary:1.0.2")
//    implementation("org.tensorflow:tensorflow-lite-gpu:latest_version")
//    implementation("org.tensorflow:tensorflow-lite:latest_version")
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.security.crypto)
    implementation(libs.play.services.mlkit.face.detection)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}