plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.example.charityapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.charityapp"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
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

    buildFeatures {
        viewBinding = true
    }
}
dependencies {
    implementation(fileTree(mapOf(
        "dir" to "C:\\Users\\HUY\\OneDrive\\Desktop\\Lirt",
        "include" to listOf("*.aar", "*.jar"),
        "exclude" to listOf<String>()
    )))
    // Khai báo chuẩn thư viện nội bộ trong thư mục app/libs
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar", "*.jar"))))
    implementation(fileTree(mapOf(
        "dir" to "C:\\Users\\Admin\\Desktop\\libs",
        "include" to listOf("*.aar", "*.jar"),
        "exclude" to listOf("")
    )))
    implementation(fileTree(mapOf(
        "dir" to "C:\\Users\\HB\\Desktop\\libs",
        "include" to listOf("*.aar", "*.jar"),
        "exclude" to listOf("")
    )))

    // Các thư viện còn lại giữ nguyên...
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.messaging)
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")

    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    implementation("com.squareup.okhttp3:okhttp:4.6.0")
    implementation("commons-codec:commons-codec:1.14")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

}