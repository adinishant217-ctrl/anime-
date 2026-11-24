package com.anistream.app.ui.screen.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anistream.app.core.common.toUserMessage
import com.anistream.app.domain.model.AnimeSummary
import com.anistream.app.domain.model.Genre
import com.anistream.app.domain.model.ScheduleDay
import com.anistream.app.domain.repository.AnimeRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ExploreMode {
    ONGOING,
    COMPLETED,
    SCHEDULE
}

data class ExploreUiState(
    val mode: ExploreMode = ExploreMode.ONGOING,
    val genres: List<Genre> = emptyList(),
    val selectedGenre: Genre? = null,
    val items: List<AnimeSummary> = emptyList(),
    val schedule: List<ScheduleDay> = emptyList(),
    val page: Int = 1,
    val hasNext: Boolean = false,
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val error: String? = null
)

class ExploreViewModel(
    private val animeRepository: AnimeRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ExploreUiState())
    val state: StateFlow<ExploreUiState> = _state.asStateFlow()

    private var requestJob: Job? = null

    init {
        loadGenres()
        load(reset = true)
    }

    fun setMode(mode: ExploreMode) {
        val current = _state.value
        if (current.mode == mode && current.selectedGenre == null) return
        _state.update {
            it.copy(
                mode = mode,
                selectedGenre = null,
                items = emptyList(),
                schedule = emptyList(),
                page = 1,
                hasNext = false
            )
        }
        load(reset = true)
    }

    fun selectGenre(genre: Genre) {
        val next = if (_state.value.selectedGenre?.slug == genre.slug) null else genre
        _state.update {
            it.copy(
                selectedGenre = next,
                items = emptyList(),
                page = 1,
                hasNext = false
            )
        }
        load(reset = true)
    }

    fun clearGenre() {
        if (_state.value.selectedGenre == null) return
        _state.update {
            it.copy(
                selectedGenre = null,
                items = emptyList(),
                page = 1,
                hasNext = false
            )
        }
        load(reset = true)
    }

    fun loadMore() {
        val current = _state.value
        if (current.loading || current.loadingMore || !current.hasNext) return
        load(reset = false)
    }

    fun retry() {
        load(reset = true)
    }

    private fun loadGenres() {
        viewModelScope.launch {
            animeRepository.getGenres().onSuccess { genres ->
                _state.update { it.copy(genres = genres) }
            }
        }
    }

    private fun load(reset: Boolean) {
        val snapshot = _state.value
        if (!reset && (snapshot.loading || snapshot.loadingMore || !snapshot.hasNext)) return
        requestJob?.cancel()
        val page = if (reset) 1 else snapshot.page + 1
        _state.update {
            it.copy(
                loading = reset,
                loadingMore = !reset,
                error = if (reset) null else it.error
            )
        }
        requestJob = viewModelScope.launch {
            val genre = snapshot.selectedGenre
            if (snapshot.mode == ExploreMode.SCHEDULE && genre == null) {
                animeRepository.getSchedule().fold(
                    onSuccess = { days ->
                        _state.update {
                            it.copy(
                                loading = false,
                                loadingMore = false,
                                schedule = days,
                                items = emptyList(),
                                hasNext = false,
                                error = null
                            )
                        }
                    },
                    onFailure = { error ->
                        _state.update {
                            it.copy(
                                loading = false,
                                loadingMore = false,
                                error = error.toUserMessage()
                            )
                        }
                    }
                )
                return@launch
            }

            val result = when {
                genre != null -> animeRepository.getGenreAnime(genre.slug, page)
                snapshot.mode == ExploreMode.COMPLETED -> animeRepository.getCompleted(page)
                else -> animeRepository.getOngoing(page)
            }

            result.fold(
                onSuccess = { paged ->
                    _state.update { current ->
                        current.copy(
                            loading = false,
                            loadingMore = false,
                            error = null,
                            items = if (reset) {
                                paged.items
                            } else {
                                (current.items + paged.items).distinctBy { it.slug }
                            },
                            page = paged.page,
                            hasNext = paged.hasNext
                        )
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            loading = false,
                            loadingMore = false,
                            error = error.toUserMessage()
                        )
                    }
                }
            )
        }
    }
}
