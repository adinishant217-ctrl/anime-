package com.anistream.app.core.common

import org.junit.Assert.assertEquals
import org.junit.Test

class EpisodeNumbersTest {

    @Test
    fun `extracts episode number from catalogue title`() {
        assertEquals(
            8,
            EpisodeNumbers.extract("Gensou Suikoden Episode 8 Subtitle Indonesia")
        )
    }

    @Test
    fun `extracts number from Eps abbreviation`() {
        assertEquals(1070, EpisodeNumbers.extract("One Piece Eps 1070 Subtitle Indonesia"))
    }

    @Test
    fun `returns zero when no episode number exists`() {
        assertEquals(0, EpisodeNumbers.extract("Anime Movie"))
    }

    @Test
    fun `cleans episode and subtitle noise`() {
        assertEquals(
            "Gensou Suikoden",
            EpisodeNumbers.cleanTitle("Gensou Suikoden Episode 8 Subtitle Indonesia", 8)
        )
    }

    @Test
    fun `clean title falls back to original when everything is removed`() {
        assertEquals("Episode 4", EpisodeNumbers.cleanTitle("Episode 4", 4))
    }
}
