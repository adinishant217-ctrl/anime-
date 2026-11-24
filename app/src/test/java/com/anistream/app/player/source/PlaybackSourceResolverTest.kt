package com.anistream.app.player.source

import com.anistream.app.TestData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackSourceResolverTest {

    private val resolver = PlaybackSourceResolver()

    @Test
    fun `embed-only episode is not playable`() {
        val resolution = resolver.resolve(TestData.embedEpisode(), null, true)

        assertFalse(resolution.isPlayable)
        assertNotNull(resolution.unavailableReason)
        assertEquals(2, resolution.providers.size)
        assertTrue(resolution.sources.isEmpty())
    }

    @Test
    fun `direct hls source is preferred at highest quality`() {
        val resolution = resolver.resolve(TestData.directEpisode(), null, true)

        assertTrue(resolution.isPlayable)
        assertEquals("https://cdn.example.com/720.m3u8", resolution.sources.first().url)
        assertEquals("720p", resolution.sources.first().qualityLabel)
    }

    @Test
    fun `preferred quality overrides the highest source`() {
        val resolution = resolver.resolve(TestData.directEpisode(), "360p", true)

        assertEquals("360p", resolution.sources.first().qualityLabel)
        assertEquals("https://cdn.example.com/360.mp4", resolution.sources.first().url)
    }

    @Test
    fun `lowest quality is selected when highest is not preferred`() {
        val resolution = resolver.resolve(TestData.directEpisode(), null, false)

        assertEquals("360p", resolution.sources.first().qualityLabel)
    }

    @Test
    fun `direct media detection accepts files and manifests only`() {
        assertTrue(PlaybackSourceResolver.isDirectMediaUrl("https://cdn.example.com/ep.mp4"))
        assertTrue(PlaybackSourceResolver.isDirectMediaUrl("https://cdn.example.com/master.m3u8?token=1"))
        assertTrue(PlaybackSourceResolver.isDirectMediaUrl("https://cdn.example.com/manifest.mpd"))
        assertFalse(PlaybackSourceResolver.isDirectMediaUrl("https://filedon.co/embed/abc"))
        assertFalse(PlaybackSourceResolver.isDirectMediaUrl("https://desustream.net/dstream/odcdn/?id=abc"))
        assertFalse(PlaybackSourceResolver.isDirectMediaUrl(""))
    }
}
