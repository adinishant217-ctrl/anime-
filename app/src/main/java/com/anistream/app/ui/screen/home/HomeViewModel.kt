package com.anistream.app.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anistream.app.core.common.toUserMessage
import com.anistream.app.domain.model.AnimeSummary
import com.anistream.app.domain.model.HistoryEntry
import com.anistream.app.domain.model.ScheduleDay
import com.anistream.app.domain.repository.AnimeRepository
import com.anistream.app.domain.repository.LibraryRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val featured: AnimeSummary? = null,
    val ongoing: List<AnimeSummary> = emptyList(),
    val completed: List<AnimeSummary> = emptyList(),
    val schedule: List<ScheduleDay> = emptyList(),
    val continueWatching: List<HistoryEntry> = emptyList()
)

class HomeViewModel(
    private val animeRepository: AnimeRepository,
    libraryRepository: LibraryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            libraryRepository.observeContinueWatching().collect { entries ->
                _state.update { it.copy(continueWatching = entries) }
            }
        }
        load()
    }

    fun load() {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            coroutineScope {
                val ongoingDeferred = async { animeRepository.getOngoing(1) }
                val completedDeferred = async { animeRepository.getCompleted(1) }
                val scheduleDeferred = async { animeRepository.getSchedule() }

                val ongoingResult = ongoingDeferred.await()
                val completedResult = completedDeferred.await()
                val scheduleResult = scheduleDeferred.await()

                val ongoing = ongoingResult.getOrNull()?.items.orEmpty()
                val completed = completedResult.getOrNull()?.items.orEmpty()
                val schedule = scheduleResult.getOrNull().orEmpty()
                val allEmpty = ongoing.isEmpty() && completed.isEmpty() && schedule.isEmpty()
                val error = if (allEmpty) {
                    ongoingResult.exceptionOrNull()?.toUserMessage()
                        ?: completedResult.exceptionOrNull()?.toUserMessage()
                        ?: scheduleResult.exceptionOrNull()?.toUserMessage()
                } else {
                    null
                }

                _state.update {
                    it.copy(
                        loading = false,
                        error = error,
                        featured = ongoing.firstOrNull() ?: completed.firstOrNull(),
                        ongoing = ongoing,
                        completed = completed,
                        schedule = schedule
                    )
                }
            }
        }
    }
}
