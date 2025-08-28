import kotlin.collections.addAll

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.aboutLibraries)
}

val supportedAbis = arrayOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")

android {
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

    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = true
        }
    }

    namespace = "top.laoxin.modmanager"
    compileSdk = 36

    ndkVersion = "28.1.13356709"

    defaultConfig {
        applicationId = "com.mod.manager"
        minSdk = 28
        targetSdk = 36
        versionCode = 382
        versionName = "3.8.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        signingConfig = signingConfigs.getByName("release")

        ndk {
            abiFilters.addAll(supportedAbis)
            debugSymbolLevel = "none"
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
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        buildConfig = true
        compose = true
        aidl = true
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

    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.rules)
    testImplementation(libs.junit)
    implementation(platform(libs.compose.bom))
    androidTestImplementation(platform(libs.compose.bom))
    // Material Design 3
    implementation(libs.material3)
    // Compose Material Design
    implementation(libs.androidx.material.icons.core)
    // Compose Material Design Extended
    implementation(libs.androidx.material.icons.extended)
    // Compose Activity
    implementation(libs.androidx.activity.compose)
    // ViewModels
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    // LiveData
    implementation(libs.androidx.runtime.livedata)
    // accompanist insets
    implementation(libs.accompanist.permissions)
    // 添加 documentfile 依赖
    implementation(libs.androidx.documentfile)
    // 添加shizuku 依赖
    implementation(libs.shizuku)
    implementation(libs.provider)
    // 添加datastore 依赖储存用户配置
    implementation(libs.androidx.datastore.preferences)
    // 添加zip4j 依赖
    implementation(libs.zip4j)
    //Room
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.roomcompiler)
    implementation(libs.androidx.room.ktx)
    // Navigation
    implementation(libs.androidx.navigation.compose)
    // gson
    implementation(libs.gson)
    //添加 documentfile 依赖 (SDK自带的那个版本有问题):
    implementation(libs.androidx.documentfile)
    // Retrofit
    implementation(libs.retrofit2.kotlinx.serialization.converter)
    implementation(libs.converter.gson)
    implementation(libs.retrofit)
    implementation(libs.okhttp3.okhttp)
    implementation(libs.coil.compose)
    implementation(libs.kotlinx.serialization.json)
    // Retrofit with Scalar Converter
    implementation(libs.converter.scalars)
    // 解压库
    implementation(libs.commons.compress)
    // xz
    implementation(libs.xz)
    // 7z
    implementation(libs.x.zip.jbinding.xandroid)
    // 系统UI控制库，实现沉浸式状态栏
    implementation(libs.accompanist.systemuicontroller)
    // Glide实现预览图压缩
    implementation(libs.glide)
    // pager2
    implementation(libs.accompanist.pager)
    // desugar
    coreLibraryDesugaring(libs.desugar)
    // 启动页
    implementation(libs.androidx.core.splashscreen)
    // DI依赖注入
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    // aboutlibraries
    implementation(libs.aboutlibraries.core)
    implementation(libs.aboutlibraries.compose.m3)
    implementation(libs.androidx.ui.tooling)
    // markdown
    implementation(libs.compose.markdown)
    // 携程核心库
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
}
