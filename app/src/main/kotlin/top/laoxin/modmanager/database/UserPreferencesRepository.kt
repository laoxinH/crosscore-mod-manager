package top.laoxin.modmanager.database

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.io.IOException


class UserPreferencesRepository(
    private val dataStore: DataStore<Preferences>
) {


    companion object {
        val PREFERENCES_KEYS = mapOf(
            // 控制台配置项
            "ANTI_HARMONY" to booleanPreferencesKey("ANTI_HARMONY"),
            "SCAN_QQ_DIRECTORY" to booleanPreferencesKey("SCAN_QQ_DIRECTORY"),
            "SELECTED_DIRECTORY" to stringPreferencesKey("SELECTED_DIRECTORY"),
            "SCAN_DOWNLOAD" to booleanPreferencesKey("SCAN_DOWNLOAD"),
            // 模组配置项
            "OPEN_PERMISSION_REQUEST_DIALOG" to booleanPreferencesKey("OPEN_PERMISSION_REQUEST_DIALOG"),
            // 配置安装位置
            "INSTALL_PATH" to stringPreferencesKey("INSTALL_PATH"),
            // 游戏服务器
            "GAME_SERVICE" to stringPreferencesKey("GAME_SERVICE"),
            // 用户提示
            "USER_TIPS" to booleanPreferencesKey("USER_TIPS"),
            // 选择的游戏
            "SELECTED_GAME" to intPreferencesKey("SELECTED_GAME"),
            // 扫描文件夹中的Mods
            "SCAN_DIRECTORY_MODS" to booleanPreferencesKey("SCAN_DIRECTORY_MODS"),
            // 删除解压目录
            "DELETE_UNZIP_DIRECTORY" to booleanPreferencesKey("DELETE_UNZIP_DIRECTORY"),
            // 展示分类视图
            "SHOW_CATEGORY_VIEW" to booleanPreferencesKey("SHOW_CATEGORY_VIEW"),
        )
        const val TAG = "UserPreferencesRepo"
    }

    suspend fun <T> savePreference(key: String, value: T) {
        val preferenceKey = PREFERENCES_KEYS[key]
        if (preferenceKey != null) {
            dataStore.edit { preferences ->
                @Suppress("UNCHECKED_CAST")
                preferences[preferenceKey as Preferences.Key<T>] = value
            }
        }
    }

    fun <T> getPreferenceFlow(key: String, defaultValue: T): Flow<T> {

        val preferenceKey = PREFERENCES_KEYS[key]
        return if (preferenceKey != null) {
            dataStore.data.catch {
                if (it is IOException) {
                    Log.e(TAG, "Error reading preferences.", it)
                    emit(emptyPreferences())
                } else {
                    throw it
                }
            }.map { preferences ->
                @Suppress("UNCHECKED_CAST")
                preferences[preferenceKey as Preferences.Key<T>] ?: defaultValue
            }
        } else {
            flowOf(defaultValue)
        }
    }
}