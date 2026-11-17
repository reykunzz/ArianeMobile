plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.ariane.mobile"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ariane.mobile"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0-dev"
        externalNativeBuild {
            cmake { cppFlags += listOf("-std=c++17", "-Wall", "-Wextra") }
        }
        ndk { abiFilters += listOf("arm64-v8a") }
    }
    externalNativeBuild { cmake { path = file("src/main/cpp/CMakeLists.txt"); version = "3.22.1" } }
}
