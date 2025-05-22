import java.io.FileInputStream // Không cần thiết nữa nếu không dùng getMapboxAccessToken
import java.util.Properties    // Không cần thiết nữa nếu không dùng getMapboxAccessToken

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
        // KHÔNG CẦN buildConfigField cho MAPBOX_PUBLIC_TOKEN nếu dùng strings.xml/secrets.xml
        // KHÔNG CẦN buildConfigField cho MAPBOX_DOWNLOADS_TOKEN (Gradle tự dùng từ gradle.properties)
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

    // buildFeatures { buildConfig = true } // Vẫn giữ nếu bạn dùng BuildConfig cho việc khác
    // Nếu bạn không dùng BuildConfig cho bất kỳ mục đích nào khác, có thể comment out dòng trên
    // để tối ưu build time. Nhưng nếu bạn đã dùng nó cho HERE_ROUTING_API_KEY trước đó
    // (mà giờ không dùng nữa) thì có thể bỏ hẳn nếu không còn buildConfigField nào khác.
    // Hiện tại, cứ để buildConfig = true cũng không sao.

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    packagingOptions {
        resources.excludes.add("META-INF/*")
    }
    // buildFeatures { buildConfig = true } // Đã có ở trên, không cần lặp lại
}


dependencies {
    implementation("io.github.chaosleung:pinview:1.4.4")
    implementation(platform("com.google.firebase:firebase-bom:33.4.0"))
    // Firebase Cloud Messaging
    implementation("com.google.firebase:firebase-messaging")
    implementation ("androidx.browser:browser:1.5.0")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-firestore")    
    implementation("com.google.firebase:firebase-functions")
    testImplementation ("org.mockito:mockito-core:4.2.0")
    implementation("com.google.firebase:firebase-appcheck-playintegrity")
    implementation("com.google.firebase:firebase-appcheck-debug")
    implementation ("com.github.bumptech.glide:glide:4.15.1")
    implementation("com.google.android.gms:play-services-vision:20.0.0")
    implementation("com.google.mlkit:text-recognition:16.0.0-beta5")
    implementation("com.github.dikamahard:FaceCompareLibrary:1.0.2")
    implementation ("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.sun.mail:android-mail:1.6.7")
    implementation("com.sun.mail:android-activation:1.6.7")
    implementation("com.google.android.gms:play-services-location:21.2.0") // Giữ lại nếu dùng FusedLocationProviderClient
    implementation ("com.google.android.material:material:1.12.0")
    implementation("com.google.android.gms:play-services-maps:18.2.0") // Kiểm tra phiên bản mới nhất
//    implementation("com.google.android.gms:play-services-places:18.2.0") // Kiểm tra phiên bản mới nhất


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