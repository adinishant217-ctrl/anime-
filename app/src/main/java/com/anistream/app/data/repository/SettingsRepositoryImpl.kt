package com.anistream.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.anistream.app.domain.model.AppSettings
import com.anistream.app.domain.model.ThemeMode
import com.anistream.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "anistream_settings")

class SettingsRepositoryImpl(
    private val context: Context
) : SettingsRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override val settings: Flow<AppSettings> = context.settingsDataStore.data
        .catch { emit(emptyPreferences()) }
        .map { preferences ->
            AppSettings(
                themeMode = preferences[Keys.THEME_MODE]
                    ?.let { stored -> ThemeMode.values().firstOrNull { it.name == stored } }
                    ?: ThemeMode.MIDNIGHT,
                autoplayNext = preferences[Keys.AUTOPLAY_NEXT] ?: true,
                preferHighestQuality = preferences[Keys.PREFER_HIGHEST_QUALITY] ?: true,
                searchHistory = decodeHistory(preferences[Keys.SEARCH_HISTORY])
            )
        }

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    override suspend fun setAutoplayNext(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.AUTOPLAY_NEXT] = enabled }
    }

    override suspend fun setPreferHighestQuality(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.PREFER_HIGHEST_QUALITY] = enabled }
    }

    override suspend fun addSearchQuery(query: String) {
        val cleaned = query.trim()
        if (cleaned.isBlank()) return
        context.settingsDataStore.edit { preferences ->
            val current = decodeHistory(preferences[Keys.SEARCH_HISTORY])
            val updated = (listOf(cleaned) + current.filterNot { it.equals(cleaned, ignoreCase = true) })
                .take(MAX_SEARCH_HISTORY)
            preferences[Keys.SEARCH_HISTORY] = json.encodeToString(updated)
        }
    }

    override suspend fun clearSearchHistory() {
        context.settingsDataStore.edit { it.remove(Keys.SEARCH_HISTORY) }
    }

    private fun decodeHistory(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<String>>(raw) }.getOrDefault(emptyList())
    }

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val AUTOPLAY_NEXT = booleanPreferencesKey("autoplay_next")
        val PREFER_HIGHEST_QUALITY = booleanPreferencesKey("prefer_highest_quality")
        val SEARCH_HISTORY = stringPreferencesKey("search_history")
    }

    private companion object {
        const val MAX_SEARCH_HISTORY = 10
    }
}
