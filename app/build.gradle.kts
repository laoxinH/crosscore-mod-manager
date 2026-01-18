plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.aboutLibraries)
    alias(libs.plugins.aboutLibrariesAndroid)
    id("kotlin-parcelize")
}

val supportedAbis = arrayOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")

android {
    namespace = "top.laoxin.modmanager"
    compileSdk = 36
    ndkVersion = "29.0.14206865"

    signingConfigs {
        create("release") {
            storeFile = file("${projectDir}/keystore/androidkey.jks")
            storePassword = "000000"
            keyAlias = "key0"
            keyPassword = "000000"
            enableV3Signing = true
            enableV4Signing = true
        }
    }

    defaultConfig {
        applicationId = "com.mod.manager"
        minSdk = 28
        targetSdk = 36
        versionCode = 503
        versionName = "5.0.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        signingConfig = signingConfigs.getByName("release")

        ndk {
            abiFilters.addAll(supportedAbis)
            debugSymbolLevel = "none"
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include(*supportedAbis)
            isUniversalApk = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
        aidl = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    packaging {
        dex {
            useLegacyPackaging = false
        }
        resources {
            excludes += listOf(
                "/META-INF/DEPENDENCIES",
                "/META-INF/LICENSE",
                "/META-INF/NOTICE",
                "/META-INF/ASL2.0",
                "/META-INF/{AL2.0,LGPL2.1}"
            )
        }
        jniLibs {
            keepDebugSymbols.addAll(
                listOf(
                    "**/lib7-Zip-JBinding.so",
                    "**/libandroidx.graphics.path.so",
                    "**/libdatastore_shared_counter.so"
                )
            )
        }
    }
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs = listOf(
            "-progressive",
            "-Xjvm-default=all",
            "-Xcontext-parameters",
            "-Xwhen-guards",
            "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.ui.ExperimentalComposeUiApi",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.animation.ExperimentalAnimationApi",
            "-opt-in=androidx.compose.animation.ExperimentalSharedTransitionApi",
            "-opt-in=kotlin.ExperimentalStdlibApi",
            "-opt-in=kotlin.contracts.ExperimentalContracts",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.coroutines.FlowPreview",
            "-opt-in=kotlinx.serialization.ExperimentalSerializationApi",
        )
    }
}

androidComponents {
    onVariants(selector().withBuildType("release")) {
        it.packaging.resources.excludes.addAll(
            "/META-INF/**",
            "/kotlin/**",
            "**.txt",
            "**.bin",
        )
    }
}

dependencies {
    implementation(libs.androidx.security.state.provider)
    implementation(libs.androidx.animation)
    implementation(libs.androidx.foundation.layout)
    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.rules)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Compose UI
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.tooling)

    // Compose Foundation & Layout
    implementation(libs.compose.foundation)
    implementation(libs.compose.foundation.layout)

    // Compose Runtime & Animation
    implementation(libs.compose.runtime)
    implementation(libs.compose.animation)
    implementation(libs.compose.animation.core)
    implementation(libs.androidx.runtime.livedata)

    // Material Design
    implementation(libs.material3)
    implementation(libs.material3.adaptive)
    implementation(libs.material3.adaptive.navigation.suite)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material.icons.extended)

    // Accompanist
    implementation(libs.accompanist.permissions)
    implementation(libs.accompanist.systemuicontroller)
    implementation(libs.accompanist.pager)

    // AndroidX Core
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.documentfile)

    // AndroidX Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)

    // AndroidX Navigation
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.navigation.runtime.ktx)

    // Navigation 3
    //implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.material3.navigation3)
    implementation(libs.androidx.navigation3.runtime)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.roomcompiler)

    // Network
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.converter.scalars)
    implementation(libs.retrofit2.kotlinx.serialization.converter)
    implementation(libs.okhttp3.okhttp)

    // Serialization
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.gson)

    // Image Loading
    implementation(libs.coil.compose)
    implementation(libs.glide)

    // Dependency Injection
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    // Compression & Archives
    implementation(libs.commons.compress)
    implementation(libs.xz)
    implementation(libs.zip4j)
    implementation(libs.x.zip.jbinding.xandroid)

    // Shizuku
    implementation(libs.shizuku)
    implementation(libs.provider)

    // UI Components
    implementation(libs.aboutlibraries.core)
    implementation(libs.aboutlibraries.compose.m3)
    implementation(libs.compose.markdown)
}