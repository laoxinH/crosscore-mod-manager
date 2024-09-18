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
    val updateBaseUrl =
        "https://github.com/laoxinH/crosscore-mod-manager/releases/download/$versionName/"
    val updatePath = "update"
    val updateInfoFilename = "update.json"
    val gameConfigPaht = "gameConfig"
    val gameConfigFilename = "gameConfig.json"

}

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.serialization)
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

    defaultConfig {
        applicationId = "com.mod.manager"
        minSdk = 28
        targetSdk = 35
        versionCode = buildInfo.versionCode
        versionName = buildInfo.versionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        signingConfig = signingConfigs.getByName("release")
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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }
    buildFeatures {
        compose = true
        aidl = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "2.0.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
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
    implementation(libs.material)
    // or Material Design 2
    //implementation("androidx.compose.material:material")
    // or skip Material Design and build directly on top of foundational components
    //implementation("androidx.compose.foundation:foundation")
    // or only import the main APIs for the underlying toolkit systems,
    // such as input and measurement/layout
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
    implementation(libs.api)
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

    implementation(libs.accompanist.pager)
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
