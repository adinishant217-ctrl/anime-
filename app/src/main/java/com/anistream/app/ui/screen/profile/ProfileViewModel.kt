package com.anistream.app.ui.screen.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anistream.app.domain.repository.LibraryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val savedCount: Int = 0,
    val historyCount: Int = 0,
    val completedCount: Int = 0,
    val watchTimeMs: Long = 0L
)

class ProfileViewModel(
    libraryRepository: LibraryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                libraryRepository.observeWatchlist(),
                libraryRepository.observeHistory()
            ) { watchlist, history ->
                ProfileUiState(
                    savedCount = watchlist.size,
                    historyCount = history.size,
                    completedCount = history.count { it.completed },
                    watchTimeMs = history.sumOf { it.positionMs }
                )
            }.collect { profile ->
                _state.update { profile }
            }
        }
    }
}
