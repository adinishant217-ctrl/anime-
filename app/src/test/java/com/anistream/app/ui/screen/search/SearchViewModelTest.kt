package com.anistream.app.ui.screen.search

import com.anistream.app.FakeAnimeRepository
import com.anistream.app.FakeSettingsRepository
import com.anistream.app.MainDispatcherRule
import com.anistream.app.TestData
import com.anistream.app.domain.model.SearchResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `debounced query loads results`() = runTest(mainDispatcherRule.testDispatcher) {
        val animeRepository = FakeAnimeRepository().apply {
            searchResult = SearchResult(
                query = "naruto",
                total = 1,
                items = listOf(TestData.summary("boruto", "Boruto"))
            )
        }
        val viewModel = SearchViewModel(animeRepository, FakeSettingsRepository())

        viewModel.onQueryChange("naruto")
        advanceTimeBy(450L)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(1, state.results.size)
        assertEquals("boruto", state.results.single().slug)
        assertEquals("naruto", state.submittedQuery)
        assertEquals(1, state.total)
    }

    @Test
    fun `submit stores the query in search history`() = runTest(mainDispatcherRule.testDispatcher) {
        val settings = FakeSettingsRepository()
        val animeRepository = FakeAnimeRepository().apply {
            searchResult = SearchResult("naruto", 0, emptyList())
        }
        val viewModel = SearchViewModel(animeRepository, settings)

        viewModel.onQueryChange("naruto")
        viewModel.onSubmit()
        advanceUntilIdle()

        assertTrue(settings.state.value.searchHistory.contains("naruto"))
        assertEquals(listOf("naruto"), viewModel.state.value.history)
    }
}
