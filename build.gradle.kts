// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.jetbrainsKotlinAndroid) apply false
    alias(libs.plugins.compose.compiler) apply false
}

buildscript {

    extra.apply {
        set("room_version", "2.6.1")
    }
    dependencies {
        classpath(libs.okhttp3.okhttp)
    }
    repositories{
        maven { url = uri("https://jitpack.io") }
    }

}


