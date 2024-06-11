import org.jetbrains.kotlin.de.undercouch.gradle.tasks.download.org.apache.commons.logging.LogFactory.release

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    id("com.google.devtools.ksp") version "1.9.20-1.0.14"
   // id("com.google.devtools.ksp") version "1.5.10-1.0.0-beta01"
}
android {

    namespace = "top.laoxin.modmanager"
    compileSdk = 34
    buildFeatures {
        compose = true
    }

        //...
    applicationVariants.all {
        outputs.all {
            val ver = defaultConfig.versionName?.replace(" ","-")
            //val minSdk = defaultConfig.minSdk
            //val abi = filters.find{it.filterType == "ABI"}?.identifier ?:"all"
            (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName =
                "ModManager-release-$ver.apk";
        }
    }


    //...

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }
    defaultConfig {
        applicationId = "top.laoxin.modmanager"
        minSdk = 26
        targetSdk = 34
        versionCode = 3
        versionName = "1.8.5 beta"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }

    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
        aidl = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }


}

dependencies {

implementation(libs.androidx.navigation.runtime.ktx)
    testImplementation("junit:junit:4.12")
    /*    implementation(libs.androidx.core.ktx)
       implementation(libs.androidx.lifecycle.runtime.ktx)
       implementation(libs.androidx.activity.compose)
       //implementation(platform(libs.androidx.compose.bom))
       implementation(libs.androidx.ui)
       implementation(libs.androidx.ui.graphics)
      // implementation(libs.androidx.ui.tooling.preview)
       implementation(libs.androidx.material3)


      testImplementation(libs.junit)
       androidTestImplementation(libs.androidx.junit)
       androidTestImplementation(libs.androidx.espresso.core)
      // androidTestImplementation(platform(libs.androidx.compose.bom))
       androidTestImplementation(libs.androidx.ui.test.junit4)
       //debugImplementation(libs.androidx.ui.tooling)
       debugImplementation(libs.androidx.ui.test.manifest)*/


    val composeBom = platform("androidx.compose:compose-bom:2024.04.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Choose one of the following:
    // Material Design 3
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material")
    // or Material Design 2
    //implementation("androidx.compose.material:material")
    // or skip Material Design and build directly on top of foundational components
    //implementation("androidx.compose.foundation:foundation")
    // or only import the main APIs for the underlying toolkit systems,
    // such as input and measurement/layout
    implementation("androidx.compose.ui:ui")

    // Android Studio Preview support
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // UI Tests
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Optional - Included automatically by material, only add when you need
    // the icons but not the material library (e.g. when using Material3 or a
    // custom design system based on Foundation)
    implementation("androidx.compose.material:material-icons-core")
    // Optional - Add full set of material icons
    implementation("androidx.compose.material:material-icons-extended")
    // Optional - Add window size utils
    implementation("androidx.compose.material3:material3-window-size-class")

    // Optional - Integration with activities
    implementation("androidx.activity:activity-compose:1.8.2")
    // Optional - Integration with ViewModels
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")
    // Optional - Integration with LiveData
    implementation("androidx.compose.runtime:runtime-livedata")
    // Optional - Integration with RxJava
    implementation("androidx.compose.runtime:runtime-rxjava2")
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")

    // 添加 documentfile 依赖
    implementation ("androidx.documentfile:documentfile:1.0.1")
    // 添加shizuku 依赖
    implementation ("dev.rikka.shizuku:api:13.1.5")
    implementation ("dev.rikka.shizuku:provider:13.1.5")

    // 添加datastore 依赖储存用户配置
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // 添加zip4j 依赖
    implementation ("net.lingala.zip4j:zip4j:2.11.5")
    //Room
    implementation("androidx.room:room-runtime:${rootProject.extra["room_version"]}")
    ksp("androidx.room:room-compiler:${rootProject.extra["room_version"]}")
    implementation("androidx.room:room-ktx:${rootProject.extra["room_version"]}")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")
    // gson
    implementation("com.google.code.gson:gson:2.10.1")
    //添加 documentfile 依赖（SDK自带的那个版本有问题）：
    implementation("androidx.documentfile:documentfile:1.0.1")

    // Retrofit
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("io.coil-kt:coil-compose:2.4.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    // Retrofit
// Retrofit with Scalar Converter
    implementation("com.squareup.retrofit2:converter-scalars:2.9.0")

}