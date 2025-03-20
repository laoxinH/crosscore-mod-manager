package top.laoxin.modmanager.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import top.laoxin.modmanager.App
import top.laoxin.modmanager.BuildConfig
import top.laoxin.modmanager.R
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "version_data_store")

@Singleton
class VersionRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val versionDataStore = context.dataStore
    private val versionKey = stringPreferencesKey("version")
    private val versionInfoKey = stringPreferencesKey("version_info")
    private val versionUrlKey = stringPreferencesKey("version_url")
    private val universalUrlKey = stringPreferencesKey("universal_url")

    // 存储版本号
    suspend fun saveVersion(version: String) {
        versionDataStore.edit { preferences ->
            preferences[versionKey] = version
        }
    }

    // 获取版本号
    suspend fun getVersion(): String {
        val preferences = versionDataStore.data.first()
        return preferences[versionKey] ?: BuildConfig.VERSION_NAME
    }

    // 存储版本信息
    suspend fun saveVersionInfo(versionInfo: String) {
        versionDataStore.edit { preferences ->
            preferences[versionInfoKey] = versionInfo
        }
    }

    // 获取版本信息
    suspend fun getVersionInfo(): String {
        val preferences = versionDataStore.data.first()
        return preferences[versionInfoKey] ?: ""
    }

    // 存储版本下载地址
    suspend fun saveVersionUrl(versionUrl: String) {
        versionDataStore.edit { preferences ->
            preferences[versionUrlKey] = versionUrl
        }
    }

    // 获取版本下载地址
    suspend fun getVersionUrl(): String {
        val preferences = versionDataStore.data.first()
        return preferences[versionUrlKey] ?: App.get()
            .getString(R.string.github_url_releases_latest)
    }

    // 存储通用下载地址
    suspend fun saveUniversalUrl(universalUrl: String) {
        versionDataStore.edit { preferences ->
            preferences[universalUrlKey] = universalUrl
        }
    }

    // 获取通用下载地址
    suspend fun getUniversalUrl(): String {
        val preferences = versionDataStore.data.first()
        return preferences[universalUrlKey] ?: App.get()
            .getString(R.string.github_url_releases_latest)
    }
}
