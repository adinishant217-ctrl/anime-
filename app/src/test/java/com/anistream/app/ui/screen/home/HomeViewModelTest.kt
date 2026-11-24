package com.anistream.app.ui.screen.home

import com.anistream.app.FakeAnimeRepository
import com.anistream.app.FakeLibraryRepository
import com.anistream.app.MainDispatcherRule
import com.anistream.app.TestData
import com.anistream.app.domain.model.PagedAnime
import com.anistream.app.domain.model.ScheduleDay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `loads ongoing completed and schedule sections`() = runTest(mainDispatcherRule.testDispatcher) {
        val animeRepository = FakeAnimeRepository().apply {
            ongoing = PagedAnime(listOf(TestData.summary("one-piece", "One Piece")), 1, false)
            completed = PagedAnime(listOf(TestData.summary("frieren", "Frieren")), 1, false)
            schedule = listOf(
                ScheduleDay("senin", "Senin", listOf(TestData.summary("monday-show", "Monday Show")))
            )
        }

        val viewModel = HomeViewModel(animeRepository, FakeLibraryRepository())
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.loading)
        assertNull(state.error)
        assertEquals("one-piece", state.featured?.slug)
        assertEquals(1, state.ongoing.size)
        assertEquals("frieren", state.completed.single().slug)
        assertEquals(1, state.schedule.size)
    }

    @Test
    fun `reports an error when every home request fails`() = runTest(mainDispatcherRule.testDispatcher) {
        val animeRepository = FakeAnimeRepository().apply { failAll = true }

        val viewModel = HomeViewModel(animeRepository, FakeLibraryRepository())
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.loading)
        assertNotNull(state.error)
        assertNull(state.featured)
        assertEquals(0, state.ongoing.size)
    }
}
