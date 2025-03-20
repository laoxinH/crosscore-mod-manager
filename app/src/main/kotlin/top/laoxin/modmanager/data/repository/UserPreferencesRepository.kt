package top.laoxin.modmanager.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import top.laoxin.modmanager.constant.UserPreferencesKeys
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFERENCE_NAME = "mod_manager_preferences"
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = PREFERENCE_NAME
)

@Singleton
class UserPreferencesRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    companion object {
        val PREFERENCES_KEYS = mapOf(
            UserPreferencesKeys.SELECTED_GAME to intPreferencesKey(UserPreferencesKeys.SELECTED_GAME),
            UserPreferencesKeys.SCAN_QQ_DIRECTORY to booleanPreferencesKey(UserPreferencesKeys.SCAN_QQ_DIRECTORY),
            UserPreferencesKeys.SELECTED_DIRECTORY to stringPreferencesKey(UserPreferencesKeys.SELECTED_DIRECTORY),
            UserPreferencesKeys.SCAN_DOWNLOAD to booleanPreferencesKey(UserPreferencesKeys.SCAN_DOWNLOAD),
            UserPreferencesKeys.OPEN_PERMISSION_REQUEST_DIALOG to booleanPreferencesKey(
                UserPreferencesKeys.OPEN_PERMISSION_REQUEST_DIALOG
            ),
            UserPreferencesKeys.SCAN_DIRECTORY_MODS to booleanPreferencesKey(UserPreferencesKeys.SCAN_DIRECTORY_MODS),
            UserPreferencesKeys.DELETE_UNZIP_DIRECTORY to booleanPreferencesKey(UserPreferencesKeys.DELETE_UNZIP_DIRECTORY),
            UserPreferencesKeys.SHOW_CATEGORY_VIEW to booleanPreferencesKey(UserPreferencesKeys.SHOW_CATEGORY_VIEW),
            UserPreferencesKeys.USER_TIPS to booleanPreferencesKey(UserPreferencesKeys.USER_TIPS)
        )
        const val TAG = "UserPreferencesRepo"
    }

    suspend fun <T> savePreference(key: String, value: T) {
        val preferenceKey = PREFERENCES_KEYS[key]
        if (preferenceKey != null) {
            context.dataStore.edit { preferences ->
                @Suppress("UNCHECKED_CAST")
                preferences[preferenceKey as Preferences.Key<T>] = value
            }
        }
    }

    fun <T> getPreferenceFlow(key: String, defaultValue: T): Flow<T> {

        val preferenceKey = PREFERENCES_KEYS[key]
        return if (preferenceKey != null) {
            context.dataStore.data.catch {
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