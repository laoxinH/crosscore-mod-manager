package top.laoxin.modmanager.data.network

import android.os.Build

object AppConfig {

    private val abi = Build.SUPPORTED_ABIS[0].takeIf {
        it in setOf("arm64-v8a", "x86_64", "armeabi-v7a", "x86")
    } ?: "universal"

    fun matchVariant(name: String) = name.contains(abi)
}
