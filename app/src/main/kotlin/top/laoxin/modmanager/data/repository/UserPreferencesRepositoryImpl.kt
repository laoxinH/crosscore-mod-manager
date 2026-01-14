package top.laoxin.modmanager.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import top.laoxin.modmanager.constant.GameInfoConstant
import top.laoxin.modmanager.constant.PathConstants
import top.laoxin.modmanager.domain.bean.GameInfoBean
import top.laoxin.modmanager.domain.model.AppError
import top.laoxin.modmanager.domain.model.Result
import top.laoxin.modmanager.domain.repository.UserPreferencesRepository
import top.laoxin.modmanager.ui.view.modView.NavigationIndex

private const val PREFERENCE_NAME = "mod_manager_preferences"
private val Context.dataStore: DataStore<Preferences> by
        preferencesDataStore(name = PREFERENCE_NAME)

@Singleton
class UserPreferencesRepositoryImpl
@Inject
constructor(
        @param:ApplicationContext private val context: Context,
        private val scope: CoroutineScope
) : UserPreferencesRepository {

        private val json = Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
        }

        private object Keys {
                // 存储完整的 GameInfoBean JSON，不再使用索引
                val SELECTED_GAME_JSON = stringPreferencesKey("selected_game_json")
                val SCAN_QQ_DIRECTORY = booleanPreferencesKey("scan_qq_directory")
                val SELECTED_DIRECTORY = stringPreferencesKey("selected_directory")
                val SCAN_DOWNLOAD = booleanPreferencesKey("scan_download")
                val SCAN_DIRECTORY_MODS = booleanPreferencesKey("scan_directory_mods")
                val DELETE_UNZIP_DIRECTORY = booleanPreferencesKey("delete_unzip_directory")
                val SHOW_CATEGORY_VIEW = booleanPreferencesKey("show_category_view")
                val USER_TIPS = booleanPreferencesKey("user_tips")
                val MODS_VIEW_INDEX = intPreferencesKey("mods_view_index")
                val MOD_LIST_DISPLAY_MODE = intPreferencesKey("mod_list_display_mode")
                val CONFLICT_DETECTION_ENABLED = booleanPreferencesKey("conflict_detection_enabled")

                // 版本缓存
                val CACHED_VERSION_NAME = stringPreferencesKey("cached_version_name")
                val CACHED_CHANGELOG = stringPreferencesKey("cached_changelog")
                val CACHED_DOWNLOAD_URL = stringPreferencesKey("cached_download_url")
                val CACHED_UNIVERSAL_URL = stringPreferencesKey("cached_universal_url")

                // 公告缓存
                val CACHED_INFORMATION = stringPreferencesKey("cached_information")
                val CACHED_INFORMATION_VISION = doublePreferencesKey("cached_information_vision")
        }

        private val dataStore = context.dataStore

        // Repository 级别的协程作用域，用于缓存数据订阅

        private fun <T> getPreference(key: Preferences.Key<T>, defaultValue: T): Flow<T> {
                return dataStore.data
                        .catch {
                                if (it is IOException) {
                                        Log.e(
                                                "UserPreferencesRepo",
                                                "Error reading preferences.",
                                                it
                                        )
                                        emit(emptyPreferences())
                                } else {
                                        throw it
                                }
                        }
                        .map { preferences -> preferences[key] ?: defaultValue }
        }

        private suspend fun <T> savePreference(key: Preferences.Key<T>, value: T) {
                dataStore.edit { preferences -> preferences[key] = value }
        }

        // ==================== 游戏选择 ====================

        /** 内部 StateFlow 缓存，用于同步访问 */
        private val _selectedGameState = MutableStateFlow(GameInfoConstant.NO_GAME)

        init {
                // 订阅 DataStore 变化并更新缓存
                scope.launch(Dispatchers.IO) {
                        dataStore
                                .data
                                .catch {
                                        if (it is IOException) {
                                                Log.e(
                                                        "UserPreferencesRepo",
                                                        "Error reading selected game.",
                                                        it
                                                )
                                                emit(emptyPreferences())
                                        } else {
                                                throw it
                                        }
                                }
                                .map { preferences ->
                                        val jsonString = preferences[Keys.SELECTED_GAME_JSON]
                                        if (jsonString.isNullOrEmpty()) {
                                                GameInfoConstant.NO_GAME
                                        } else {
                                                try {
                                                        json.decodeFromString<GameInfoBean>(
                                                                jsonString
                                                        )
                                                } catch (e: Exception) {
                                                        Log.e(
                                                                "UserPreferencesRepo",
                                                                "Failed to parse selected game JSON",
                                                                e
                                                        )
                                                        GameInfoConstant.NO_GAME
                                                }
                                        }
                                }
                                .collect { gameInfo -> _selectedGameState.value = gameInfo }
                }
        }

        /** 当前选中的游戏 (Flow 版本，用于响应式订阅) */
        override val selectedGame: Flow<GameInfoBean> = _selectedGameState.asStateFlow()

        /** 同步获取当前选中的游戏 (用于命令型 UseCase) */
        override val selectedGameValue: GameInfoBean
                get() = _selectedGameState.value

        /** 保存选中的游戏 将 GameInfoBean 序列化为 JSON 字符串存储 */
        override suspend fun saveSelectedGame(gameInfo: GameInfoBean) {
                // Log.d("UserPreferencesRepo", "Saving selected game: ${gameInfo.modSavePath}")
                val jsonString =
                        try {

                                // Log.d("UserPreferencesRepo", "修复后的 JSON: ${gameInfo}")
                                Gson().toJson(gameInfo)
                        } catch (e: Exception) {
                                Log.e("UserPreferencesRepo", "Failed to encode game to JSON", e)
                                ""
                        }
                savePreference(Keys.SELECTED_GAME_JSON, jsonString)
        }

        // ==================== 目录设置 ====================

        override suspend fun prepareAndSetModDirectory(
                selectedDirectoryPath: String,
        ): Result<Unit> {
                return withContext(Dispatchers.IO) {
                        try {
                                val currentGame = selectedGameValue
                                val gameConfigFile =
                                        File(
                                                (PathConstants.ROOT_PATH +
                                                                "/$selectedDirectoryPath/" +
                                                                PathConstants.GAME_CONFIG_PATH)
                                                        .replace("tree", "")
                                                        .replace("//", "/")
                                        )

                                if (gameConfigFile.absolutePath.contains(
                                                "${PathConstants.ROOT_PATH}/Android"
                                        )
                                ) {
                                        return@withContext Result.Error(
                                                AppError.FileError.PermissionDenied
                                        )
                                }

                                val gameModsFile =
                                        File(
                                                (PathConstants.ROOT_PATH +
                                                                "/$selectedDirectoryPath/" +
                                                                currentGame.packageName)
                                                        .replace("tree", "")
                                                        .replace("//", "/")
                                        )

                                gameConfigFile.mkdirs()
                                if (currentGame.packageName.isNotEmpty()) {
                                        gameModsFile.mkdirs()
                                        saveSelectedGame(
                                                currentGame.copy(
                                                        modSavePath = gameModsFile.absolutePath
                                                )
                                        )
                                }

                                saveSelectedDirectory(
                                        "/$selectedDirectoryPath/"
                                                .replace("tree", "")
                                                .replace("//", "/")
                                )

                                Result.Success(Unit)
                        } catch (e: Exception) {
                                Result.Error(
                                        AppError.FileError.Unknown(
                                                e.message ?: "Failed to prepare mod directory"
                                        )
                                )
                        }
                }
        }

        override val selectedDirectory: Flow<String> =
                getPreference(Keys.SELECTED_DIRECTORY, PathConstants.DOWNLOAD_MOD_PATH)
        override suspend fun saveSelectedDirectory(path: String) =
                savePreference(Keys.SELECTED_DIRECTORY, path)

        override val scanQQDirectory: Flow<Boolean> = getPreference(Keys.SCAN_QQ_DIRECTORY, false)
        override suspend fun saveScanQQDirectory(shouldScan: Boolean) =
                savePreference(Keys.SCAN_QQ_DIRECTORY, shouldScan)

        override val scanDownload: Flow<Boolean> = getPreference(Keys.SCAN_DOWNLOAD, false)
        override suspend fun saveScanDownload(shouldScan: Boolean) =
                savePreference(Keys.SCAN_DOWNLOAD, shouldScan)

        override val scanDirectoryMods: Flow<Boolean> =
                getPreference(Keys.SCAN_DIRECTORY_MODS, true)
        override suspend fun saveScanDirectoryMods(shouldScan: Boolean) =
                savePreference(Keys.SCAN_DIRECTORY_MODS, shouldScan)

        override val deleteUnzipDirectory: Flow<Boolean> =
                getPreference(Keys.DELETE_UNZIP_DIRECTORY, false)
        override suspend fun saveDeleteUnzipDirectory(shouldDelete: Boolean) =
                savePreference(Keys.DELETE_UNZIP_DIRECTORY, shouldDelete)

        // ==================== UI 设置 ====================

        override val showCategoryView: Flow<Boolean> = getPreference(Keys.SHOW_CATEGORY_VIEW, true)
        override suspend fun saveShowCategoryView(shouldShow: Boolean) =
                savePreference(Keys.SHOW_CATEGORY_VIEW, shouldShow)

        override val userTips: Flow<Boolean> = getPreference(Keys.USER_TIPS, true)
        override suspend fun saveUserTips(shouldShow: Boolean) =
                savePreference(Keys.USER_TIPS, shouldShow)

        override val modsViewIndex: Flow<Int> =
                getPreference(Keys.MODS_VIEW_INDEX, NavigationIndex.MODS_BROWSER.index)
        override suspend fun saveModsViewIndex(index: Int) =
                savePreference(Keys.MODS_VIEW_INDEX, index)

        override val modListDisplayMode: Flow<Int> = getPreference(Keys.MOD_LIST_DISPLAY_MODE, 0)
        override suspend fun saveModListDisplayMode(mode: Int) =
                savePreference(Keys.MOD_LIST_DISPLAY_MODE, mode)

        override val conflictDetectionEnabled: Flow<Boolean> =
                getPreference(Keys.CONFLICT_DETECTION_ENABLED, true)
        override suspend fun saveConflictDetectionEnabled(enabled: Boolean) =
                savePreference(Keys.CONFLICT_DETECTION_ENABLED, enabled)

        // ==================== 版本缓存 ====================

        override val cachedVersionName: Flow<String> = getPreference(Keys.CACHED_VERSION_NAME, "")
        override suspend fun saveCachedVersionName(name: String) =
                savePreference(Keys.CACHED_VERSION_NAME, name)

        override val cachedChangelog: Flow<String> = getPreference(Keys.CACHED_CHANGELOG, "")
        override suspend fun saveCachedChangelog(log: String) =
                savePreference(Keys.CACHED_CHANGELOG, log)

        override val cachedDownloadUrl: Flow<String> = getPreference(Keys.CACHED_DOWNLOAD_URL, "")
        override suspend fun saveCachedDownloadUrl(url: String) =
                savePreference(Keys.CACHED_DOWNLOAD_URL, url)

        override val cachedUniversalUrl: Flow<String> = getPreference(Keys.CACHED_UNIVERSAL_URL, "")
        override suspend fun saveCachedUniversalUrl(url: String) =
                savePreference(Keys.CACHED_UNIVERSAL_URL, url)

        // ==================== 公告缓存 ====================

        override val cachedInformationVision: Flow<Double> =
                getPreference(Keys.CACHED_INFORMATION_VISION, 0.0)
        override val cachedInformation: Flow<String> = getPreference(Keys.CACHED_INFORMATION, "")
        override suspend fun saveCachedInformation(information: String) =
                savePreference(Keys.CACHED_INFORMATION, information)
        override suspend fun saveCachedInformationVision(vision: Double) =
                savePreference(Keys.CACHED_INFORMATION_VISION, vision)
}
