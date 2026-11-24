package com.anistream.app.ui.screen.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anistream.app.core.common.toUserMessage
import com.anistream.app.domain.model.AnimeDetail
import com.anistream.app.domain.model.HistoryEntry
import com.anistream.app.domain.repository.AnimeRepository
import com.anistream.app.domain.repository.LibraryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetailsUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val detail: AnimeDetail? = null,
    val isInWatchlist: Boolean = false,
    val history: Map<Int, HistoryEntry> = emptyMap(),
    val watchlistBusy: Boolean = false
) {
    val resumeEntry: HistoryEntry?
        get() = history.values
            .filter { it.hasProgress && !it.completed }
            .maxByOrNull { it.updatedAt }
}

class DetailsViewModel(
    private val animeSlug: String,
    private val animeRepository: AnimeRepository,
    private val libraryRepository: LibraryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DetailsUiState())
    val state: StateFlow<DetailsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            libraryRepository.observeIsInWatchlist(animeSlug).collect { saved ->
                _state.update { it.copy(isInWatchlist = saved) }
            }
        }
        viewModelScope.launch {
            libraryRepository.observeHistoryForAnime(animeSlug).collect { entries ->
                _state.update { it.copy(history = entries.associateBy { entry -> entry.episodeNumber }) }
            }
        }
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            animeRepository.getAnimeDetail(animeSlug).fold(
                onSuccess = { detail ->
                    _state.update { it.copy(loading = false, detail = detail, error = null) }
                },
                onFailure = { error ->
                    _state.update { it.copy(loading = false, error = error.toUserMessage()) }
                }
            )
        }
    }

    fun toggleWatchlist() {
        val detail = _state.value.detail ?: return
        if (_state.value.watchlistBusy) return
        viewModelScope.launch {
            _state.update { it.copy(watchlistBusy = true) }
            val save = !_state.value.isInWatchlist
            runCatching {
                libraryRepository.setWatchlistSaved(
                    saved = save,
                    slug = detail.slug,
                    title = detail.title,
                    coverUrl = detail.coverUrl,
                    status = detail.status,
                    type = detail.type
                )
            }
            _state.update { it.copy(watchlistBusy = false) }
        }
    }
}
