plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.posters"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.posters"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            // This is the most important line. It enables code shrinking with R8.
            isMinifyEnabled = true

            // This enables resource shrinking (removes unused images, layouts, etc.)
            // It only works if isMinifyEnabled is true.
            isShrinkResources = true

            // This tells the shrinker which rules to follow to avoid breaking your app.
            // The default file is usually enough for most projects.
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
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
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // For loading images from URLs
    implementation("io.coil-kt:coil-compose:2.5.0")

    // The foundation library is needed for LazyVerticalStaggeredGrid.
    // This replaces the older, more problematic layout-specific dependencies.
    implementation("androidx.compose.foundation:foundation:1.6.8") // Or latest version

    //Add the Google Sign-In library
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // For networking
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0") // To parse JSON

    // For ViewModel lifecycle management
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")

// Add this line to get access to more icons like 'Downloading'
    implementation("androidx.compose.material:material-icons-extended-android:1.6.8") // Or the latest version
}