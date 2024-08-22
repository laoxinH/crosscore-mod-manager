import groovy.json.JsonBuilder
import groovy.json.JsonSlurper

object buildInfo {
    val versionCode = 22
    val versionName = "3.0.1"
    val versionDes = versionName + " 更新\n" +
            "1.调整最低安卓版本为安卓9\n" +
            "2.修改更新渠道为github\n" +
            "3.微调大屏设备横屏布局\n" +
            "注意!!正式版本修改了包名,如果安装更新后会显示两个实验室app请关掉旧版的MOD并卸载\n" +
            "注意!!正式版本修改了包名,如果安装更新后会显示两个实验室app请关掉旧版的MOD并卸载\n"
    val updateBaseUrl = "https://github.com/laoxinH/crosscore-mod-manager/releases/download/$versionName/"
    val updatePath = "update"
    val updateInfoFilename = "update.json"
    val gameConfigPaht = "gameConfig"
    val gameConfigFilename = "gameConfig.json"

}

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    id("com.google.devtools.ksp") version "1.9.23-1.0.20"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.10"
}

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

    namespace = "top.laoxin.modmanager"
    compileSdk = 34
    buildFeatures {
        compose = true
    }

    applicationVariants.all {
        outputs.all {
            (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName =
                "ModManager-release.apk"
        }

        if (this.buildType.name == "release") {
            this.assembleProvider.get().doLast {
                generateUpdateInfo("ModManager-release.apk")
                generateGameConfigApi()
            }
        }
    }

    defaultConfig {
        applicationId = "com.mod.manager"
        minSdk = 28
        targetSdk = 34
        versionCode = buildInfo.versionCode
        versionName = buildInfo.versionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
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

    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        aidl = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.rules)
    testImplementation(libs.junit)

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
    implementation("androidx.activity:activity-compose:1.9.1")
    // Optional - Integration with ViewModels
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")
    // Optional - Integration with LiveData
    implementation("androidx.compose.runtime:runtime-livedata")
    // Optional - Integration with RxJava
    implementation("androidx.compose.runtime:runtime-rxjava2")
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")

    // 添加 documentfile 依赖
    implementation("androidx.documentfile:documentfile:1.0.1")
    // 添加shizuku 依赖
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")

    // 添加datastore 依赖储存用户配置
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // 添加zip4j 依赖
    implementation("net.lingala.zip4j:zip4j:2.11.5")
    //Room
    implementation("androidx.room:room-runtime:${rootProject.extra["room_version"]}")
    ksp("androidx.room:room-compiler:${rootProject.extra["room_version"]}")
    implementation("androidx.room:room-ktx:${rootProject.extra["room_version"]}")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")
    // gson
    implementation("com.google.code.gson:gson:2.11.0")
    //添加 documentfile 依赖（SDK自带的那个版本有问题）：
    implementation("androidx.documentfile:documentfile:1.0.1")

    // Retrofit
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    // Retrofit
    // Retrofit with Scalar Converter
    implementation("com.squareup.retrofit2:converter-scalars:2.11.0")
    // 解压库
    implementation("org.apache.commons:commons-compress:1.27.1")
    implementation("org.tukaani:xz:1.10")
    // 7z
    implementation("com.github.omicronapps:7-Zip-JBinding-4Android:Release-16.02-2.02")

    // 系统UI控制库，实现沉浸式状态栏
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.34.0")
}

// 计算apk的md5
fun generateMD5(file: File): String? {
    println("generateMD5: $file")
    return file.name
}


fun generateUpdateInfo(apkName: String) {
    println("------------------ Generating version info ------------------")
    println("------------------ 开始生成apk信息 ------------------")
    // 把apk文件从build目录复制到根项目的update文件夹下
    val apkFile = project.file("build/outputs/apk/release/$apkName")
    if (!apkFile.exists()) {
        //throw  GradleScriptException("hhh")
        println("apk file not found.")
    }

    val toDir = rootProject.file(buildInfo.updatePath)
    val apkHash = generateMD5(apkFile)
    val updateJsonFile = File(toDir, buildInfo.updateInfoFilename)
    var writeNewFile = true

    // 如果有以前的json文件，检查这次打包是否有改变
    if (updateJsonFile.exists()) {
        try {
            val oldUpdateInfo = JsonSlurper().parse(updateJsonFile) as Map<*, *>
            if (buildInfo.versionCode <= oldUpdateInfo["code"] as Int && apkHash == oldUpdateInfo["md5"] as String) {
                writeNewFile = false
            }
        } catch (e: Exception) {
            writeNewFile = true
            e.printStackTrace()
            updateJsonFile.delete()
        }
    }

    if (writeNewFile) {
        toDir.listFiles()?.forEach {
            if (!it.delete()) {
                it.deleteOnExit()
            }
        }
        copy {
            from(apkFile)
            into(toDir)
        }

        // 创建json的实体类
        // Expando可以简单理解为Map
        val updateInfo = mutableMapOf(
            "code" to buildInfo.versionCode,
            "name" to buildInfo.versionName,
            "filename" to apkFile.name,
            "url" to "${buildInfo.updateBaseUrl}${apkFile.name}",
            "time" to System.currentTimeMillis(),
            "des" to buildInfo.versionDes,
            "size" to apkFile.length(),
            "md5" to apkHash
        )
        val newApkHash = generateMD5(File(toDir, apkName))
        println("new apk md5: $newApkHash")
        val outputJson = JsonBuilder(updateInfo).toPrettyString()
        // println(outputJson)
        // 将json写入文件中，用于查询更新
        updateJsonFile.writeText(outputJson)
    } else {
        // 不需要更新
        println(
            "This version is already released.\n" +
                    "VersionCode = ${buildInfo.versionCode}\n" +
                    "Skip generateUpdateInfo."
        )
    }
    println("------------------ Finish Generating version info ------------------")
}

fun generateGameConfigApi() {
    val gameConfigDir = rootProject.file(buildInfo.gameConfigPaht)
    val gameConfigList: MutableList<Map<String, String>> = mutableListOf()
    gameConfigDir.listFiles()?.forEach {
        if (it.name.endsWith(".json")) {
            try {
                val gameConfigMap = JsonSlurper().parse(it) as Map<*, *>
                val gameConfig = mutableMapOf(
                    "gameName" to gameConfigMap["gameName"] as String,
                    "packageName" to gameConfigMap["packageName"] as String,
                    "serviceName" to gameConfigMap["serviceName"] as String,
                    "downloadUrl" to "",
                )
                gameConfigList.add(gameConfig)
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }
    val gameConfigFile =
        rootProject.file(buildInfo.gameConfigPaht + "/api/" + buildInfo.gameConfigFilename)
    val outputJson = JsonBuilder(gameConfigList).toPrettyString()
    gameConfigFile.writeText(outputJson)

}