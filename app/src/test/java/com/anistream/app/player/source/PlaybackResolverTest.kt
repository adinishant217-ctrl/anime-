package com.anistream.app.player.source

import com.anistream.app.TestData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackResolverTest {

    private val resolver = PlaybackResolver()

    @Test
    fun `embed-only episode uses provider-page fallback`() {
        val resolution = resolver.resolve(TestData.embedEpisode(), null, true)

        assertTrue(resolution.isPlayable)
        assertTrue(resolution.isEmbedded)
        assertEquals(PlaybackKind.EMBEDDED, resolution.kind)
        assertTrue(resolution.sources.isEmpty())
        assertEquals(3, resolution.embeddedSources.size)
        assertEquals("https://filedon.co/embed/R7Asi8tDrH", resolution.embeddedSources.first().url)
        assertNotNull(resolution.headers["Referer"])
        assertTrue(resolution.headers["User-Agent"].orEmpty().isNotBlank())
    }

    @Test
    fun `embedded provider source carries request headers`() {
        val resolution = resolver.resolve(TestData.embedEpisode(), null, true)
        val source = resolution.embeddedSources.first()

        assertEquals("https://filedon.co/embed/R7Asi8tDrH", source.url)
        assertEquals("360p", source.qualityLabel)
        assertEquals(
            "https://otakudesu.blog/episode/anime-a-episode-1/",
            source.headers["Referer"]
        )
        assertTrue(source.headers["User-Agent"].orEmpty().contains("Android"))
    }

    @Test
    fun `direct hls source is preferred at highest quality`() {
        val resolution = resolver.resolve(TestData.directEpisode(), null, true)

        assertTrue(resolution.isPlayable)
        assertTrue(resolution.isNative)
        assertEquals(PlaybackKind.NATIVE, resolution.kind)
        assertEquals("https://cdn.example.com/720.m3u8", resolution.sources.first().url)
        assertEquals("720p", resolution.sources.first().qualityLabel)
        assertEquals("application/x-mpegURL", resolution.sources.first().mimeType)
        assertTrue(resolution.embeddedSources.isEmpty())
    }

    @Test
    fun `preferred quality overrides the highest source`() {
        val resolution = resolver.resolve(TestData.directEpisode(), "360p", true)

        assertEquals("360p", resolution.sources.first().qualityLabel)
        assertEquals("https://cdn.example.com/360.mp4", resolution.sources.first().url)
        assertEquals("video/mp4", resolution.sources.first().mimeType)
    }

    @Test
    fun `lowest quality is selected when highest is not preferred`() {
        val resolution = resolver.resolve(TestData.directEpisode(), null, false)

        assertEquals("360p", resolution.sources.first().qualityLabel)
    }

    @Test
    fun `preferred embedded quality is ordered first`() {
        val resolution = resolver.resolve(TestData.embedEpisode(), "360p", false)

        assertEquals("360p", resolution.embeddedSources.first().qualityLabel)
    }

    @Test
    fun `direct media detection accepts files and manifests only`() {
        assertTrue(PlaybackResolver.isDirectMediaUrl("https://cdn.example.com/ep.mp4"))
        assertTrue(PlaybackResolver.isDirectMediaUrl("https://cdn.example.com/master.m3u8?token=1"))
        assertTrue(PlaybackResolver.isDirectMediaUrl("https://cdn.example.com/manifest.mpd"))
        assertTrue(PlaybackResolver.isDirectMediaUrl("https://cdn.example.com/episode.MKV"))
        assertFalse(PlaybackResolver.isDirectMediaUrl("https://filedon.co/embed/abc"))
        assertFalse(PlaybackResolver.isDirectMediaUrl("https://desustream.net/dstream/odcdn/?id=abc"))
        assertFalse(PlaybackResolver.isDirectMediaUrl(""))
    }

    @Test
    fun `classifies provider pages, direct media and invalid links`() {
        assertEquals(
            SourceKind.DIRECT_MEDIA,
            resolver.classify("https://cdn.example.com/master.m3u8")
        )
        assertEquals(
            SourceKind.PROVIDER_PAGE,
            resolver.classify("https://filedon.co/embed/abc")
        )
        assertEquals(
            SourceKind.PROVIDER_PAGE,
            resolver.classify("https://desustream.net/dstream/odcdn/?id=abc")
        )
        assertEquals(SourceKind.INVALID, resolver.classify("https://example.com/watch"))
        assertEquals(SourceKind.INVALID, resolver.classify("javascript:alert(1)"))
        assertEquals(SourceKind.INVALID, resolver.classify(""))
    }

    @Test
    fun `episode with no usable links is explicitly unavailable`() {
        val resolution = resolver.resolve(
            TestData.embedEpisode().copy(
                defaultStreamUrl = null,
                qualities = emptyList()
            ),
            null,
            true
        )

        assertFalse(resolution.isPlayable)
        assertEquals(PlaybackKind.UNAVAILABLE, resolution.kind)
        assertNotNull(resolution.unavailableReason)
        assertTrue(resolution.embeddedSources.isEmpty())
    }

    @Test
    fun `mime types are reported for native containers`() {
        assertEquals("application/dash+xml", PlaybackResolver.mimeTypeFor("https://x.test/a.mpd"))
        assertEquals("video/webm", PlaybackResolver.mimeTypeFor("https://x.test/a.webm?t=1"))
        assertNull(PlaybackResolver.mimeTypeFor("https://x.test/embed/a"))
    }
}
