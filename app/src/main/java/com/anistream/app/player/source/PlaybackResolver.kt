package com.anistream.app.player.source

import com.anistream.app.domain.model.EpisodeDetail
import com.anistream.app.domain.model.StreamProvider
import java.net.URI

enum class PlaybackKind {
    NATIVE,
    EMBEDDED,
    UNAVAILABLE
}

enum class SourceKind {
    DIRECT_MEDIA,
    PROVIDER_PAGE,
    INVALID
}

data class SubtitleSource(
    val url: String,
    val label: String,
    val language: String? = null
)

data class DirectPlaybackSource(
    val url: String,
    val qualityLabel: String,
    val providerName: String,
    val headers: Map<String, String> = emptyMap(),
    val mimeType: String? = null
)

/**
 * A third-party provider page. It is loaded in a WebView, top-level, exactly
 * as the provider's own player page. No HTML is scraped and no media URL is
 * extracted from it.
 */
data class EmbeddedPlaybackSource(
    val url: String,
    val qualityLabel: String,
    val providerName: String,
    val headers: Map<String, String> = emptyMap()
)

data class PlaybackResolution(
    val kind: PlaybackKind,
    val sources: List<DirectPlaybackSource>,
    val embeddedSources: List<EmbeddedPlaybackSource>,
    val providers: List<StreamProvider>,
    val headers: Map<String, String>,
    val subtitles: List<SubtitleSource> = emptyList(),
    val unavailableReason: String? = null
) {
    val isPlayable: Boolean get() = kind != PlaybackKind.UNAVAILABLE
    val isNative: Boolean get() = kind == PlaybackKind.NATIVE
    val isEmbedded: Boolean get() = kind == PlaybackKind.EMBEDDED
}

/**
 * Resolves the sources returned by the catalogue into one of three plans:
 *
 * 1. Native Media3 playback for a direct HLS, DASH or progressive URL.
 * 2. A provider-page fallback for a third-party embed player. The page is
 *    rendered by the provider in a WebView, with the provider's own controls.
 * 3. An explicit unavailable result.
 *
 * The resolver never scrapes a provider page, derives media URLs from player
 * JavaScript, or attempts to bypass authentication, DRM, anti-bot challenges
 * or geographic restrictions.
 */
class PlaybackResolver(
    private val userAgent: String = DEFAULT_USER_AGENT
) {

    fun resolve(
        episode: EpisodeDetail,
        preferredQuality: String?,
        preferHighestQuality: Boolean
    ): PlaybackResolution {
        // The episode page is the documented referrer for the provider player.
        val referer = episode.url?.takeIf { it.isHttpUrl() }
            ?: episode.defaultStreamUrl?.let { originOf(it) }
        val headers = PlaybackHeaders(referer = referer, userAgent = userAgent).asMap()

        val directSources = mutableListOf<DirectPlaybackSource>()
        val embeddedSources = mutableListOf<EmbeddedPlaybackSource>()

        episode.qualities.forEach { quality ->
            quality.providers.forEach { provider ->
                when (classify(provider.url)) {
                    SourceKind.DIRECT_MEDIA -> {
                        if (directSources.none { it.url == provider.url }) {
                            directSources += DirectPlaybackSource(
                                url = provider.url,
                                qualityLabel = quality.label,
                                providerName = provider.name,
                                headers = headers,
                                mimeType = mimeTypeFor(provider.url)
                            )
                        }
                    }

                    SourceKind.PROVIDER_PAGE -> {
                        if (embeddedSources.none { it.url == provider.url }) {
                            embeddedSources += EmbeddedPlaybackSource(
                                url = provider.url,
                                qualityLabel = quality.label,
                                providerName = provider.name,
                                headers = headers
                            )
                        }
                    }

                    SourceKind.INVALID -> Unit
                }
            }
        }

        val defaultUrl = episode.defaultStreamUrl
        if (!defaultUrl.isNullOrBlank() && defaultUrl.isHttpUrl()) {
            when (classify(defaultUrl)) {
                SourceKind.DIRECT_MEDIA -> {
                    if (directSources.none { it.url == defaultUrl }) {
                        directSources += DirectPlaybackSource(
                            url = defaultUrl,
                            qualityLabel = "Default",
                            providerName = "Default stream",
                            headers = headers,
                            mimeType = mimeTypeFor(defaultUrl)
                        )
                    }
                }

                SourceKind.PROVIDER_PAGE -> {
                    if (embeddedSources.none { it.url == defaultUrl }) {
                        embeddedSources += EmbeddedPlaybackSource(
                            url = defaultUrl,
                            qualityLabel = "Default",
                            providerName = "Default stream",
                            headers = headers
                        )
                    }
                }

                SourceKind.INVALID -> Unit
            }
        }

        val orderedNative = orderSources(directSources, preferredQuality, preferHighestQuality)
        val orderedEmbedded = orderEmbeddedSources(
            embeddedSources,
            preferredQuality,
            preferHighestQuality
        )
        val allProviders = episode.qualities
            .flatMap { it.providers }
            .distinctBy { it.name to it.url }

        val kind = when {
            orderedNative.isNotEmpty() -> PlaybackKind.NATIVE
            orderedEmbedded.isNotEmpty() -> PlaybackKind.EMBEDDED
            else -> PlaybackKind.UNAVAILABLE
        }

        return PlaybackResolution(
            kind = kind,
            sources = orderedNative,
            embeddedSources = orderedEmbedded,
            providers = allProviders,
            headers = headers,
            unavailableReason = when (kind) {
                PlaybackKind.NATIVE,
                PlaybackKind.EMBEDDED -> null

                PlaybackKind.UNAVAILABLE -> if (allProviders.isEmpty() && defaultUrl.isNullOrBlank()) {
                    "The catalogue returned no stream sources for this episode."
                } else {
                    "The catalogue returned only source links that AniStream cannot open."
                }
            }
        )
    }

    fun classify(url: String): SourceKind {
        if (!url.isHttpUrl()) return SourceKind.INVALID
        if (isDirectMediaUrl(url)) return SourceKind.DIRECT_MEDIA
        // Only load pages that carry a player/embed signal. A generic HTML page
        // must never be presented as if it were a playable video.
        return if (isLikelyPlayerPage(url)) SourceKind.PROVIDER_PAGE else SourceKind.INVALID
    }

    fun orderSources(
        sources: List<DirectPlaybackSource>,
        preferredQuality: String?,
        preferHighestQuality: Boolean
    ): List<DirectPlaybackSource> = orderByQuality(
        items = sources,
        qualityOf = { it.qualityLabel },
        preferredQuality = preferredQuality,
        preferHighestQuality = preferHighestQuality
    )

    fun orderEmbeddedSources(
        sources: List<EmbeddedPlaybackSource>,
        preferredQuality: String?,
        preferHighestQuality: Boolean
    ): List<EmbeddedPlaybackSource> = orderByQuality(
        items = sources,
        qualityOf = { it.qualityLabel },
        preferredQuality = preferredQuality,
        preferHighestQuality = preferHighestQuality
    )

    fun resolutionOf(label: String): Int =
        Regex("""\d+""").find(label)?.value?.toIntOrNull() ?: 0

    private fun <T> orderByQuality(
        items: List<T>,
        qualityOf: (T) -> String,
        preferredQuality: String?,
        preferHighestQuality: Boolean
    ): List<T> {
        if (items.isEmpty()) return emptyList()
        val preferred = preferredQuality?.let { wanted ->
            items.firstOrNull { qualityOf(it).equals(wanted, ignoreCase = true) }
                ?: items.firstOrNull {
                    qualityOf(it).contains(wanted, ignoreCase = true) ||
                        wanted.contains(qualityOf(it), ignoreCase = true)
                }
        }
        if (preferred != null) {
            val preferredIndex = items.indexOf(preferred)
            return listOf(preferred) + items.filterIndexed { index, _ -> index != preferredIndex }
        }
        val sorted = items.sortedByDescending { resolutionOf(qualityOf(it)) }
        return if (preferHighestQuality) sorted else sorted.reversed()
    }

    companion object {
        const val DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

        private val directExtensions = listOf(
            ".mp4",
            ".m4v",
            ".m3u8",
            ".mpd",
            ".mkv",
            ".webm",
            ".mov"
        )

        private val playerPageHints = listOf(
            "/embed",
            "/player",
            "player/",
            "/e/",
            "/v/",
            "/iframe",
            "/dstream",
            "video.g"
        )

        fun isLikelyPlayerPage(url: String): Boolean {
            val normalized = url.lowercase()
            return playerPageHints.any { normalized.contains(it) }
        }

        fun isDirectMediaUrl(url: String): Boolean {
            if (url.isBlank()) return false
            val path = runCatching { URI(url).path.orEmpty() }.getOrDefault(url)
                .lowercase()
            return directExtensions.any { path.endsWith(it) }
        }

        fun mimeTypeFor(url: String): String? {
            val path = runCatching { URI(url).path.orEmpty() }.getOrDefault(url)
                .substringBefore('?')
                .substringBefore('#')
                .lowercase()
            return when {
                path.endsWith(".m3u8") -> "application/x-mpegURL"
                path.endsWith(".mpd") -> "application/dash+xml"
                path.endsWith(".mp4") || path.endsWith(".m4v") -> "video/mp4"
                path.endsWith(".mkv") -> "video/x-matroska"
                path.endsWith(".webm") -> "video/webm"
                path.endsWith(".mov") -> "video/quicktime"
                else -> null
            }
        }

        private fun String.isHttpUrl(): Boolean = runCatching {
            val uri = URI(this)
            uri.scheme?.lowercase() in setOf("http", "https") &&
                !uri.host.isNullOrBlank()
        }.getOrDefault(false)

        private fun originOf(url: String): String? = runCatching {
            val uri = URI(url)
            if (uri.scheme?.lowercase() !in setOf("http", "https")) return@runCatching null
            val port = if (uri.port > 0) ":${uri.port}" else ""
            "${uri.scheme}://${uri.host}$port/"
        }.getOrNull()
    }
}

private data class PlaybackHeaders(
    val referer: String?,
    val userAgent: String
) {
    fun asMap(): Map<String, String> = buildMap {
        if (!referer.isNullOrBlank()) put("Referer", referer)
        put("User-Agent", userAgent)
    }
}
