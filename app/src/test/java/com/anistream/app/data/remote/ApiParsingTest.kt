package com.anistream.app.data.remote

import com.anistream.app.core.common.ApiException
import com.anistream.app.data.remote.dto.AnimeDetailDto
import com.anistream.app.data.remote.dto.AnimeListDto
import com.anistream.app.data.remote.dto.EpisodeDetailDto
import com.anistream.app.data.remote.dto.GenreDto
import com.anistream.app.data.remote.dto.ProviderDto
import com.anistream.app.data.remote.dto.ScheduleItemDto
import com.anistream.app.data.remote.dto.SearchDataDto
import com.anistream.app.data.remote.mapper.toDomain
import com.anistream.app.data.remote.mapper.toScheduleDays
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiParsingTest {

    private val json = defaultApiJson()

    @Test
    fun `parses ongoing list and pagination`() {
        val dto = decodeApiData<AnimeListDto>(json, ONGOING_JSON)
        val items = dto.list.orEmpty().map { it.toDomain() }

        assertEquals(2, items.size)
        assertEquals("genso-suikoden-sub-indo", items[0].slug)
        assertEquals("Episode 8", items[0].episodeLabel)
        assertEquals("Sabtu", items[0].day)
        assertNull(items[1].coverUrl)
        assertEquals("12 Episode", items[1].totalEpisodes)
        assertEquals(20, dto.pagination?.per_page)
    }

    @Test
    fun `maps detail metadata, genres and sorted episodes`() {
        val dto = decodeApiData<AnimeDetailDto>(json, DETAIL_JSON)
        val detail = dto.toDomain()

        assertEquals("Gensou Suikoden", detail.title)
        assertEquals("幻想水滸伝", detail.japaneseTitle)
        assertNull(detail.score)
        assertNull(detail.releaseDate)
        assertEquals(2, detail.genres.size)
        assertEquals("action", detail.genres[0].slug)
        assertEquals(2, detail.episodes.size)
        assertEquals(7, detail.episodes[0].number)
        assertEquals(8, detail.episodes[1].number)
        assertEquals("Gensou Suikoden", detail.episodes[0].displayTitle)
        assertFalse(detail.hasBatch)
    }

    @Test
    fun `maps episode providers and nullable neighbours`() {
        val dto = decodeApiData<EpisodeDetailDto>(json, EPISODE_JSON)
        val episode = dto.toDomain()

        assertEquals("gsskd-episode-8-sub-indo", episode.slug)
        assertEquals("gsskd-episode-7-sub-indo", episode.previousEpisodeSlug)
        assertNull(episode.nextEpisodeSlug)
        assertEquals(3, episode.providerCount)
        assertEquals(2, episode.qualities.size)
        assertEquals("https://desustream.net/embed/x", episode.defaultStreamUrl)
    }

    @Test
    fun `decodes html escaped provider query delimiters`() {
        val provider = decodeApiData<ProviderDto>(json, ESCAPED_PROVIDER_JSON).toDomain()

        assertEquals(
            "https://www.blogger.com/video.g?token=abc&origin=example.blogspot.com",
            provider.url
        )
    }

    @Test
    fun `parses search results with string genres`() {
        val dto = decodeApiData<SearchDataDto>(json, SEARCH_JSON)
        val result = dto.toDomain()

        assertEquals("naruto", result.query)
        assertEquals(1, result.total)
        assertEquals(1, result.items.size)
        assertEquals(listOf("Action", "Adventure"), result.items[0].genres)
    }

    @Test
    fun `orders schedule days from monday to sunday`() {
        val dto = decodeApiData<Map<String, List<ScheduleItemDto>>>(json, SCHEDULE_JSON)
        val days = dto.toScheduleDays()

        assertEquals(2, days.size)
        assertEquals("senin", days[0].key)
        assertEquals("Senin", days[0].label)
        assertEquals("minggu", days[1].key)
    }

    @Test
    fun `parses genre array`() {
        val genres = decodeApiData<List<GenreDto>>(json, GENRES_JSON).map { it.toDomain() }

        assertEquals(1, genres.size)
        assertEquals("Action", genres[0].name)
        assertEquals("action", genres[0].slug)
    }

    @Test
    fun `rejects unsuccessful envelopes with a readable message`() {
        val error = assertThrows(ApiException::class.java) {
            decodeApiData<AnimeListDto>(json, ERROR_JSON)
        }
        assertTrue(error.message.orEmpty().contains("Endpoint tidak ditemukan"))
        assertTrue(error.message.orEmpty().contains("404"))
    }

    private companion object {
        const val ONGOING_JSON = """
        {
          "meta": {"status": true, "code": 200, "message": "Success"},
          "data": {
            "list": [
              {
                "title": "Gensou Suikoden",
                "slug": "genso-suikoden-sub-indo",
                "url": "https://otakudesu.blog/anime/genso-suikoden-sub-indo/",
                "cover": "https://otakudesu.blog/wp-content/uploads/2026/10/160352.jpg",
                "day": "Sabtu",
                "episode": "Episode 8",
                "date": "25 Nov"
              },
              {
                "title": "No Cover Show",
                "slug": "no-cover-show",
                "url": "https://otakudesu.blog/anime/no-cover-show/",
                "total_episode": "12 Episode",
                "rating": "7.4",
                "status": "Completed"
              }
            ],
            "pagination": {"current_page": 1, "per_page": 20}
          }
        }
        """

        const val DETAIL_JSON = """
        {
          "meta": {"status": true, "code": 200, "message": "Success"},
          "data": {
            "title": "Gensou Suikoden",
            "japanese": "幻想水滸伝",
            "skor": "",
            "produser": "Frontier Works",
            "tipe": "TV",
            "status": "Ongoing",
            "total_episode": "23",
            "durasi": "24 Menit",
            "tanggal_rilis": "",
            "studio": "Konami animation",
            "genre": [
              {"name": "Action", "url": "https://otakudesu.blog/genres/action/"},
              {"name": "Fantasy", "url": "https://otakudesu.blog/genres/fantasy/"}
            ],
            "synopsis": "Synopsis here",
            "slug": "genso-suikoden-sub-indo",
            "url": "https://otakudesu.blog/anime/genso-suikoden-sub-indo/",
            "cover": "https://otakudesu.blog/wp-content/uploads/2026/10/160352.jpg",
            "batch": [],
            "episode_list": [
              {
                "title": "Gensou Suikoden Episode 8 Subtitle Indonesia",
                "slug": "gsskd-episode-8-sub-indo",
                "url": "https://otakudesu.blog/episode/gsskd-episode-8-sub-indo/",
                "date": "25 November,2026"
              },
              {
                "title": "Gensou Suikoden Episode 7 Subtitle Indonesia",
                "slug": "gsskd-episode-7-sub-indo",
                "url": "https://otakudesu.blog/episode/gsskd-episode-7-sub-indo/",
                "date": "17 November,2026"
              }
            ]
          }
        }
        """

        const val EPISODE_JSON = """
        {
          "meta": {"status": true, "code": 200, "message": "Success"},
          "data": {
            "title": "Gensou Suikoden Episode 8 Subtitle Indonesia",
            "slug": "gsskd-episode-8-sub-indo",
            "url": "https://otakudesu.blog/episode/gsskd-episode-8-sub-indo/",
            "waktu_rilis": "12:20 am",
            "prev_episode": {
              "slug": "gsskd-episode-7-sub-indo",
              "url": "https://otakudesu.blog/episode/gsskd-episode-7-sub-indo/"
            },
            "next_episode": [],
            "defaultstreaming": "https://desustream.net/embed/x",
            "stream": [
              {
                "quality": "360p",
                "providers": [
                  {"provider": "filedon", "url": "https://filedon.co/embed/R7Asi8tDrH"},
                  {"provider": "vidhide", "url": "https://odvidhide.com/embed/qtnvaj9abr60"}
                ]
              },
              {
                "quality": "720p",
                "providers": [
                  {"provider": "hls", "url": "https://cdn.example.com/ep8/master.m3u8"}
                ]
              }
            ],
            "downloads": []
          }
        }
        """

        const val ESCAPED_PROVIDER_JSON = """
        {
          "meta": {"status": true, "code": 200, "message": "Success"},
          "data": {
            "provider": "blogs",
            "url": "https://www.blogger.com/video.g?token=abc&amp;origin=example.blogspot.com"
          }
        }
        """

        const val SEARCH_JSON = """
        {
          "meta": {"status": true, "code": 200, "message": "Success"},
          "data": {
            "query": "naruto",
            "total": 1,
            "list": [
              {
                "title": "Boruto: Naruto Next Generations Subtitle Indonesia",
                "slug": "borot-sub-indo",
                "url": "https://otakudesu.blog/anime/borot-sub-indo/",
                "cover": "https://otakudesu.blog/wp-content/uploads/2020/05/Boruto-Sub-Indo.jpg",
                "status": "Drop",
                "rating": "6.15",
                "genre": ["Action", "Adventure"]
              }
            ]
          }
        }
        """

        const val SCHEDULE_JSON = """
        {
          "meta": {"status": true, "code": 200, "message": "Success"},
          "data": {
            "minggu": [{"title": "B Show", "slug": "b-show", "url": "https://example.com/b"}],
            "senin": [{"title": "A Show", "slug": "a-show", "url": "https://example.com/a"}]
          }
        }
        """

        const val GENRES_JSON = """
        {
          "meta": {"status": true, "code": 200, "message": "Success"},
          "data": [{"name": "Action", "slug": "action", "url": "/genres/action/"}]
        }
        """

        const val ERROR_JSON = """
        {
          "meta": {
            "status": false,
            "code": 404,
            "message": "Endpoint tidak ditemukan.",
            "timestamp": "2026-11-24 19:55:59"
          },
          "data": null
        }
        """
    }
}
