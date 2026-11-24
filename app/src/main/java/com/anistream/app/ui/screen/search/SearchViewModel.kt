package com.anistream.app.ui.screen.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anistream.app.core.common.toUserMessage
import com.anistream.app.domain.model.AnimeSummary
import com.anistream.app.domain.repository.AnimeRepository
import com.anistream.app.domain.repository.SettingsRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val submittedQuery: String = "",
    val results: List<AnimeSummary> = emptyList(),
    val total: Int = 0,
    val loading: Boolean = false,
    val error: String? = null,
    val history: List<String> = emptyList(),
    val hasSearched: Boolean = false
)

@OptIn(FlowPreview::class)
class SearchViewModel(
    private val animeRepository: AnimeRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    private val queryFlow = MutableStateFlow("")
    private var lastExecutedQuery: String? = null

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _state.update { it.copy(history = settings.searchHistory) }
            }
        }
        viewModelScope.launch {
            queryFlow
                .debounce(DEBOUNCE_MS)
                .map { it.trim() }
                .distinctUntilChanged()
                .collectLatest { query ->
                    if (query.length < MIN_QUERY_LENGTH) {
                        lastExecutedQuery = null
                        _state.update {
                            it.copy(
                                loading = false,
                                error = null,
                                results = emptyList(),
                                total = 0,
                                hasSearched = false
                            )
                        }
                        return@collectLatest
                    }
                    if (query == lastExecutedQuery) return@collectLatest
                    performSearch(query)
                }
        }
    }

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query) }
        queryFlow.value = query
    }

    fun onSubmit() {
        val query = _state.value.query.trim()
        if (query.length < MIN_QUERY_LENGTH) return
        viewModelScope.launch { settingsRepository.addSearchQuery(query) }
        viewModelScope.launch { performSearch(query) }
    }

    fun onHistorySelected(query: String) {
        _state.update { it.copy(query = query) }
        queryFlow.value = query
        onSubmit()
    }

    fun retry() {
        val query = _state.value.submittedQuery.ifBlank { _state.value.query }.trim()
        if (query.length < MIN_QUERY_LENGTH) return
        viewModelScope.launch { performSearch(query) }
    }

    fun clearHistory() {
        viewModelScope.launch { settingsRepository.clearSearchHistory() }
    }

    private suspend fun performSearch(query: String) {
        lastExecutedQuery = query
        _state.update {
            it.copy(
                loading = true,
                error = null,
                submittedQuery = query,
                hasSearched = true
            )
        }
        animeRepository.search(query).fold(
            onSuccess = { result ->
                _state.update {
                    it.copy(
                        loading = false,
                        results = result.items,
                        total = result.total,
                        error = null
                    )
                }
            },
            onFailure = { error ->
                _state.update {
                    it.copy(
                        loading = false,
                        results = emptyList(),
                        total = 0,
                        error = error.toUserMessage()
                    )
                }
            }
        )
    }

    private companion object {
        const val DEBOUNCE_MS = 400L
        const val MIN_QUERY_LENGTH = 2
    }
}
