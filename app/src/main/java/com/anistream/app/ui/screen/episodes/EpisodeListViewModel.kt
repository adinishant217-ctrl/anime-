package com.anistream.app.ui.screen.episodes

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

data class EpisodeListUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val detail: AnimeDetail? = null,
    val history: Map<Int, HistoryEntry> = emptyMap()
)

class EpisodeListViewModel(
    private val animeSlug: String,
    private val animeRepository: AnimeRepository,
    libraryRepository: LibraryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(EpisodeListUiState())
    val state: StateFlow<EpisodeListUiState> = _state.asStateFlow()

    init {
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
                    _state.update { it.copy(loading = false, detail = detail) }
                },
                onFailure = { error ->
                    _state.update { it.copy(loading = false, error = error.toUserMessage()) }
                }
            )
        }
    }
}
