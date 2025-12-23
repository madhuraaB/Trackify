plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.trackify"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.trackify"
        minSdk = 24
        targetSdk = 36
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
    // These 'libs.' references are likely the cause of the "Unresolved reference: libs" error,
    // but we must keep them unless you fix your libs.versions.toml file.
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("androidx.core:core:1.13.1")

    // FIX: Using the correct, standard string format for external dependencies
    // to bypass the 'libs' resolution issue and directly declare the chart library.
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
}
