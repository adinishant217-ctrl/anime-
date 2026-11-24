package com.anistream.app.ui.screen.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anistream.app.domain.model.HistoryEntry
import com.anistream.app.domain.model.SavedAnime
import com.anistream.app.domain.repository.LibraryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LibraryUiState(
    val loading: Boolean = true,
    val watchlist: List<SavedAnime> = emptyList(),
    val history: List<HistoryEntry> = emptyList()
)

class LibraryViewModel(
    private val libraryRepository: LibraryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(LibraryUiState())
    val state: StateFlow<LibraryUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                libraryRepository.observeWatchlist(),
                libraryRepository.observeHistory()
            ) { watchlist, history ->
                watchlist to history
            }
                .catch { emit(emptyList<SavedAnime>() to emptyList<HistoryEntry>()) }
                .collect { (watchlist, history) ->
                    _state.update {
                        it.copy(loading = false, watchlist = watchlist, history = history)
                    }
                }
        }
    }

    fun removeFromWatchlist(slug: String) {
        viewModelScope.launch {
            libraryRepository.setWatchlistSaved(
                saved = false,
                slug = slug,
                title = "",
                coverUrl = null,
                status = null,
                type = null
            )
        }
    }

    fun removeHistoryEntry(entry: HistoryEntry) {
        viewModelScope.launch {
            libraryRepository.removeHistoryEntry(entry.animeSlug, entry.episodeNumber)
        }
    }

    fun clearHistory() {
        viewModelScope.launch { libraryRepository.clearHistory() }
    }
}
