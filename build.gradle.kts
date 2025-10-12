plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.jetbrainsKotlinAndroid) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.aboutLibraries) apply false
    alias(libs.plugins.aboutLibrariesAndroid) apply false
}

buildscript {
    extra.apply {
        set("room_version", "2.6.0")
    }

    dependencies {
        classpath(libs.okhttp3.okhttp)

    }
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://jitpack.io") }
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://jitpack.io") }
    }
}
