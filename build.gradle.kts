// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.jetbrainsKotlinAndroid) apply false
    // id("com.android.library") version "8.1.4" apply false
}

buildscript {
    extra.apply {
        set("room_version", "2.6.0")
    }
    /* dependencies {
         val kotlin_version
         classpath ("org.jetbrains.kotlin:kotlin-android-extensions:$kotlin_version")
    }*/
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
