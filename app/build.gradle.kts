import java.io.FileInputStream
import java.util.Properties

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
        // <<<<<< THÊM DÒNG NÀY ĐỂ ĐỊNH NGHĨA BIẾN TRONG BUILDCONFIG >>>>>>
        buildConfigField("String", "DIRECTIONS_API_KEY", "\"${getApiKey("GOOGLE_DIRECTIONS_API_KEY")}\"")
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

    packagingOptions {
        resources.excludes.add("META-INF/*")
    }
    buildFeatures {
        buildConfig = true
    }
}
// <<<<<< THÊM HÀM NÀY Ở NGOÀI KHỐI android { ... } >>>>>>
// Hàm để đọc key từ local.properties
// Hàm getApiKey viết bằng Kotlin
fun getApiKey(propertyName: String): String {
    val properties = Properties()
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        try {
            properties.load(FileInputStream(localPropertiesFile))
            return properties.getProperty(propertyName, "")
        } catch (e: Exception) {
            println("Warning: Could not load local.properties file. ${e.message}")
        }
    } else {
        println("Warning: local.properties file not found. API keys might not be loaded.")
    }
    return ""
}
dependencies {
    implementation("io.github.chaosleung:pinview:1.4.4")

    implementation(platform("com.google.firebase:firebase-bom:33.4.0"))
    implementation ("androidx.browser:browser:1.5.0")
    // Thêm các phụ thuộc các dịch vụ cụ thể của Firebase
    implementation("com.google.firebase:firebase-auth") // Firebase Authentication
    implementation("com.google.firebase:firebase-storage") // Firebase Storage
    implementation("com.google.firebase:firebase-firestore") // Firebase Firestore

    implementation("com.google.firebase:firebase-appcheck-playintegrity")
    implementation("com.google.firebase:firebase-appcheck-debug")
    implementation ("com.github.bumptech.glide:glide:4.15.1")
    implementation("com.google.android.gms:play-services-vision:20.0.0")
    implementation("com.google.mlkit:text-recognition:16.0.0-beta5")
    implementation("com.github.dikamahard:FaceCompareLibrary:1.0.2")
    implementation ("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.sun.mail:android-mail:1.6.7")
    implementation("com.sun.mail:android-activation:1.6.7")
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.2.0")
    implementation ("com.google.android.material:material:1.12.0")
    implementation ("com.squareup.retrofit2:retrofit:2.9.0'")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation ("com.google.maps.android:android-maps-utils:2.3.0")
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