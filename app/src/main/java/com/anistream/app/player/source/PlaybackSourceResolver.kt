package com.anistream.app.player.source

import com.anistream.app.domain.model.EpisodeDetail
import com.anistream.app.domain.model.StreamProvider
import java.net.URI

data class DirectPlaybackSource(
    val url: String,
    val qualityLabel: String,
    val providerName: String
)

data class PlaybackResolution(
    val sources: List<DirectPlaybackSource>,
    val providers: List<StreamProvider>,
    val unavailableReason: String?
) {
    val isPlayable: Boolean get() = sources.isNotEmpty()
}

/**
 * The catalogue returns third-party embed pages (filedon, vidhide, mega,
 * desustream, blogger) rather than media files. Media3 cannot render those
 * players, and AniStream must not scrape or bypass them. This resolver accepts
 * only URLs that point directly at a progressive/HLS/DASH stream.
 */
class PlaybackSourceResolver {

    fun resolve(
        episode: EpisodeDetail,
        preferredQuality: String?,
        preferHighestQuality: Boolean
    ): PlaybackResolution {
        val directSources = buildList {
            episode.qualities.forEach { quality ->
                quality.providers.forEach { provider ->
                    if (isDirectMediaUrl(provider.url)) {
                        add(
                            DirectPlaybackSource(
                                url = provider.url,
                                qualityLabel = quality.label,
                                providerName = provider.name
                            )
                        )
                    }
                }
            }
            val defaultUrl = episode.defaultStreamUrl
            if (defaultUrl != null && isDirectMediaUrl(defaultUrl) &&
                none { it.url == defaultUrl }
            ) {
                add(
                    DirectPlaybackSource(
                        url = defaultUrl,
                        qualityLabel = "Default",
                        providerName = "Default stream"
                    )
                )
            }
        }

        val ordered = orderSources(directSources, preferredQuality, preferHighestQuality)
        val allProviders = episode.qualities.flatMap { it.providers }.distinctBy { it.name to it.url }

        return PlaybackResolution(
            sources = ordered,
            providers = allProviders,
            unavailableReason = if (ordered.isEmpty()) NO_DIRECT_SOURCE_MESSAGE else null
        )
    }

    fun orderSources(
        sources: List<DirectPlaybackSource>,
        preferredQuality: String?,
        preferHighestQuality: Boolean
    ): List<DirectPlaybackSource> {
        if (sources.isEmpty()) return emptyList()
        val preferred = preferredQuality?.let { wanted ->
            sources.firstOrNull { it.qualityLabel.equals(wanted, ignoreCase = true) }
                ?: sources.firstOrNull {
                    it.qualityLabel.contains(wanted, ignoreCase = true) ||
                        wanted.contains(it.qualityLabel, ignoreCase = true)
                }
        }
        if (preferred != null) {
            return listOf(preferred) + sources.filterNot { it.url == preferred.url }
        }
        val sorted = sources.sortedByDescending { resolutionOf(it.qualityLabel) }
        return if (preferHighestQuality) sorted else sorted.reversed()
    }

    fun resolutionOf(label: String): Int =
        Regex("""\d+""").find(label)?.value?.toIntOrNull() ?: 0

    companion object {
        private val directExtensions = listOf(".mp4", ".m3u8", ".mpd", ".mkv", ".webm", ".mov")

        const val NO_DIRECT_SOURCE_MESSAGE =
            "This episode only has third-party embed players. AniStream does not extract or " +
                "bypass those players, so there is no direct source the native player can play."

        fun isDirectMediaUrl(url: String): Boolean {
            if (url.isBlank()) return false
            val path = runCatching { URI(url).path.orEmpty() }.getOrDefault(url)
                .lowercase()
            if (directExtensions.any { path.endsWith(it) }) return true
            return path.contains(".m3u8") || path.contains(".mpd")
        }
    }
}
