package com.anistream.app.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anistream.app.domain.model.AppSettings
import com.anistream.app.domain.model.ThemeMode
import com.anistream.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val loaded: Boolean = false
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val state: StateFlow<SettingsUiState> = settingsRepository.settings
        .map { SettingsUiState(settings = it, loaded = true) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState()
        )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setAutoplayNext(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAutoplayNext(enabled) }
    }

    fun setPreferHighestQuality(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setPreferHighestQuality(enabled) }
    }

    fun clearSearchHistory() {
        viewModelScope.launch { settingsRepository.clearSearchHistory() }
    }
}
