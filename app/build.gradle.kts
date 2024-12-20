import kotlin.collections.addAll

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.serialization)
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
    compileSdk = 35

    ndkVersion = "28.0.12433566 rc1"

    defaultConfig {
        applicationId = "com.mod.manager"
        minSdk = 28
        targetSdk = 35
        versionCode = 32
        versionName = "3.2.1-patch"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        signingConfig = signingConfigs.getByName("release")

        ndk {
            abiFilters.addAll(supportedAbis)
            debugSymbolLevel = "FULL"
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
            ndk {
                debugSymbolLevel = "NONE"
            }
            signingConfig = signingConfigs.getByName("release")
        }
        getByName("debug") {
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
        viewBinding = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "2.0.0"
    }

    packaging {
        dex {
            useLegacyPackaging = false
        }
        resources {
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE"
            excludes += "/META-INF/NOTICE"
            excludes += "/META-INF/ASL2.0"
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs = listOf(
            "-progressive",
            "-Xjvm-default=all",
            "-Xcontext-receivers",
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
    // Choose one of the following:
    // Material Design 3
    implementation(libs.material3)
    // Material Design 2
    implementation(libs.androidx.compose.ui.ui)
    // Android Studio Preview support
    implementation(libs.androidx.compose.ui.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.ui.tooling)
    // UI Tests
    androidTestImplementation(libs.ui.test.junit4)
    debugImplementation(libs.ui.test.manifest)
    // Optional - Included automatically by material, only add when you need
    // the icons but not the material library (e.g. when using Material3 or a
    // custom design system based on Foundation)
    implementation(libs.androidx.material.icons.core)
    // Optional - Add full set of material icons
    implementation(libs.androidx.material.icons.extended)
    // Optional - Add window size utils
    implementation(libs.androidx.material3.window.size)
    // Optional - Integration with activities
    implementation(libs.androidx.activity.compose)
    // Optional - Integration with ViewModels
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    // Optional - Integration with LiveData
    implementation(libs.androidx.runtime.livedata)
    // Optional - Integration with RxJava
    implementation(libs.androidx.runtime.rxjava2)
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
    // Retrofit
    // Retrofit with Scalar Converter
    implementation(libs.converter.scalars)
    // 解压库
    implementation(libs.commons.compress)
    implementation(libs.xz)
    // 7z
    implementation(libs.x.zip.jbinding.xandroid)
    // 系统UI控制库，实现沉浸式状态栏
    implementation(libs.accompanist.systemuicontroller)
    // Glide实现预览图压缩
    implementation(libs.glide)
    // pager2
    implementation(libs.accompanist.pager)
    // 依赖注入
    coreLibraryDesugaring(libs.desugar)
}
