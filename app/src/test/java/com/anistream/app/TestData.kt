package com.anistream.app

import com.anistream.app.domain.model.AnimeDetail
import com.anistream.app.domain.model.AnimeSummary
import com.anistream.app.domain.model.Episode
import com.anistream.app.domain.model.EpisodeDetail
import com.anistream.app.domain.model.Genre
import com.anistream.app.domain.model.StreamProvider
import com.anistream.app.domain.model.StreamQuality

object TestData {

    fun summary(
        slug: String = "anime-a",
        title: String = "Anime A"
    ): AnimeSummary = AnimeSummary(
        slug = slug,
        title = title,
        coverUrl = "https://example.com/$slug.jpg",
        rating = "7.5",
        status = "Ongoing"
    )

    fun episode(number: Int = 1): Episode = Episode(
        slug = "anime-a-episode-$number",
        title = "Anime A Episode $number Subtitle Indonesia",
        number = number,
        date = "1 Jan",
        displayTitle = "Anime A"
    )

    fun detail(): AnimeDetail = AnimeDetail(
        slug = "anime-a",
        title = "Anime A",
        japaneseTitle = "アニメA",
        coverUrl = "https://example.com/anime-a.jpg",
        status = "Ongoing",
        type = "TV",
        totalEpisodes = "12",
        studio = "Studio A",
        synopsis = "A test synopsis.",
        genres = listOf(Genre("Action", "action")),
        episodes = listOf(episode(1), episode(2))
    )

    fun directEpisode(): EpisodeDetail = EpisodeDetail(
        slug = "anime-a-episode-1",
        title = "Anime A Episode 1 Subtitle Indonesia",
        nextEpisodeSlug = "anime-a-episode-2",
        qualities = listOf(
            StreamQuality(
                label = "360p",
                providers = listOf(
                    StreamProvider("cdn", "https://cdn.example.com/360.mp4")
                )
            ),
            StreamQuality(
                label = "720p",
                providers = listOf(
                    StreamProvider("hls", "https://cdn.example.com/720.m3u8")
                )
            )
        )
    )

    fun embedEpisode(): EpisodeDetail = EpisodeDetail(
        slug = "anime-a-episode-1",
        title = "Anime A Episode 1 Subtitle Indonesia",
        nextEpisodeSlug = null,
        defaultStreamUrl = "https://desustream.net/embed/abc",
        qualities = listOf(
            StreamQuality(
                label = "360p",
                providers = listOf(
                    StreamProvider("filedon", "https://filedon.co/embed/R7Asi8tDrH"),
                    StreamProvider("vidhide", "https://odvidhide.com/embed/qtnvaj9abr60")
                )
            )
        )
    )
}
