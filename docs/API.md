# Verified catalogue API reference

AniStream talks to **ShivraAPI** at `https://shivraapi.my.id/otd`, the same data
source used by the `anichi-cli` reference project. The upstream catalogue is
Otakudesu (`https://otakudesu.blog/`). Everything below was checked against the
live service on **24 November 2026** and against `anichi-cli/src/api.ts`,
`types.ts`, `index.ts` and `player.ts`.

The API requires no authentication, keys or cookies. `anichi-cli` enforces a
600 ms gap between requests and caches responses for one hour. AniStream uses
Retrofit/OkHttp with a 25 MB HTTP cache, a 400 ms search debounce and no
background polling. It does not attempt to defeat rate limits.

## Envelope

```json
{
  "meta": {
    "status": true,
    "code": 200,
    "message": "Success",
    "timestamp": "2026-11-24 19:56:00",
    "source": "https://otakudesu.blog/"
  },
  "data": { }
}
```

`meta.status == false` means the request failed even with HTTP 200; `meta.message`
is shown to the user. `data` is `null` on failure.

## Endpoints

| Method | Path | Query | Verified result |
| --- | --- | --- | --- |
| GET | `/otd` | — | **404** `Endpoint tidak ditemukan.` The CLI's `getHome()` is unusable. |
| GET | `/otd/ongoing` | `page` (1-based) | Anime list + pagination. |
| GET | `/otd/completed` | `page` (1-based) | Anime list + pagination. |
| GET | `/otd/anime/{slug}` | — | Full detail + `episode_list`. |
| GET | `/otd/episode/{episode-slug}` | — | Episode metadata, stream providers, downloads. |
| GET | `/otd/search` | `q` | Result list; **no page parameter**. |
| GET | `/otd/genres` | — | Genre array in `data` (not an object). |
| GET | `/otd/genres/{genre-slug}` | `page` (1-based) | Anime list + pagination. |
| GET | `/otd/schedule` | — | Map keyed by Indonesian day names. |
| GET | `/otd/batch/{slug}` | — | Batch download metadata; not used by AniStream playback. |
| — | server/quality endpoint | — | **Does not exist.** `anichi-cli.getServer()` is a hard-coded failure stub. |

## List item (`ongoing`, `completed`, `genres/{slug}`)

```json
{
  "title": "Gensou Suikoden",
  "slug": "genso-suikoden-sub-indo",
  "url": "https://otakudesu.blog/anime/genso-suikoden-sub-indo/",
  "cover": "https://otakudesu.blog/wp-content/uploads/2026/10/160352.jpg",
  "day": "Sabtu",
  "episode": "Episode 8",
  "date": "25 Nov",
  "total_episode": "12 Episode",
  "rating": "7.48",
  "status": "Ongoing",
  "studio": "...",
  "sinopsis": "...",
  "genre": [{"name": "Action", "url": "..."}]
}
```

`pagination` only contains `current_page` and `per_page`; there is **no total
page count**. AniStream treats a full page as "possibly more" and stops when a
page comes back empty.

## Anime detail

Fields actually returned: `title`, `japanese`, `skor`, `produser`, `tipe`,
`status`, `total_episode`, `durasi`, `tanggal_rilis`, `studio`, `genre[]`,
`synopsis`, `slug`, `url`, `cover`, `batch`, `episode_list[]`.

Episode items: `title`, `slug`, `url`, `date`.

Observed quirks:

- `batch` is `[]` for many titles and an object with `slug` for others.
- `episode_list` is ordered newest-first; AniStream sorts ascending by the
  episode number parsed from the title.
- `skor` and `tanggal_rilis` are frequently empty strings.
- There is **no banner/backdrop, season, year, audio or language field**. The
  poster `cover` is reused as the backdrop, and the UI only shows fields that
  are non-empty.

## Episode detail

```json
{
  "title": "Gensou Suikoden Episode 8 Subtitle Indonesia",
  "slug": "gsskd-episode-8-sub-indo",
  "url": "https://otakudesu.blog/episode/...",
  "waktu_rilis": "12:20 am",
  "prev_episode": {"slug": "gsskd-episode-7-sub-indo", "url": "..."},
  "next_episode": [],
  "defaultstreaming": "https://desustream.net/dstream/odcdn/?id=...",
  "stream": [
    {
      "quality": "360p",
      "providers": [
        {"provider": "filedon", "url": "https://filedon.co/embed/R7Asi8tDrH"},
        {"provider": "vidhide", "url": "https://odvidhide.com/embed/qtnvaj9abr60"},
        {"provider": "mega", "url": "https://mega.nz/embed/..."}
      ]
    }
  ],
  "downloads": [
    {
      "resolution": "Mp4 360p",
      "size": "40.2 MB",
      "links": [{"provider": "Pdrain", "url": "https://link.desustream.com/?id=..."}]
    }
  ]
}
```

Observed quirks:

- `next_episode` can be `[]` **or** an object; `prev_episode` is normally an
  object. Both are handled generically.
- `stream[].providers[]` are third-party **embed player pages** (`filedon`,
  `vidhide`, `mega`, `odcdn`, `odstream`, `desustream`, Blogger video).
- `defaultstreaming` is also an embed page.
- No subtitle tracks, audio tracks, DRM metadata or direct media URLs are
  returned.
- Downloads are file-hosting pages behind `link.desustream.com` redirects.

## Search

```json
{
  "query": "naruto",
  "total": 1,
  "list": [
    {
      "title": "Boruto: Naruto Next Generations Subtitle Indonesia",
      "slug": "borot-sub-indo",
      "url": "...",
      "cover": "...",
      "status": "Drop",
      "rating": "6.15",
      "genre": ["Action", "Adventure"]
    }
  ]
}
```

`genre` here is an array of **strings**, unlike the detail and genre endpoints
which use objects. The mapper handles both. Search has no pagination.

## Schedule

`data` is an object keyed by `senin`, `selasa`, `rabu`, `kamis`, `jumat`,
`sabtu`, `minggu`; each value is an array of `{title, url, slug}`. Some days can
be missing.

## Playback capability

The original CLI's real behaviour for these providers is important:

- `src/index.ts` checks whether a provider URL contains `/embed/` or `player`
  and, for those URLs, opens the page in the desktop browser. It does not hand
  them to MPV.
- `playUrl` in `src/player.ts` only invokes MPV for other URLs. MPV's optional
  yt-dlp hook is a desktop-only extractor and is not an Android player.
- When MPV/browser playback is unavailable, the CLI opens the provider page in
  the browser. That is an embedded-player fallback, not native extraction.

AniStream reproduces that documented workflow on Android:

- Direct HTTP(S) media URLs ending in `.mp4`, `.m4v`, `.m3u8`, `.mpd`, `.mkv`,
  `.webm` or `.mov` are played by Media3 ExoPlayer. Requests include the
  episode-page `Referer` and a browser-compatible `User-Agent`; HLS, DASH and
  progressive MIME types are supplied where known. Redirects, timeouts,
  cancellation and one re-resolve after an expired-URL error are handled.
- Recognized provider pages (`/embed`, `/player`, `/e/`, `/v/`, `/iframe`,
  `/dstream`, `video.g`) are loaded top-level in the provider's own WebView
  player. The resolver deliberately rejects an arbitrary HTML page instead of
  presenting it as video. The user can switch providers, go fullscreen (when the
  provider requests it), or open the page in the browser.
- Anything else is explicitly unavailable. The app does not scrape, extract,
  proxy or bypass DRM, authentication, anti-bot challenges, geo-restrictions or
  provider access controls.

Key limitations:

- Media3 cannot render embed HTML; the WebView fallback keeps provider control.
- No direct media, subtitle list or audio list is supplied by the API.
- `downloads[].links[]` point at file hosts behind redirects, not stable direct
  media URLs.
- Provider pages can reject embedded playback. In a CI-server check on
  24 Nov 2026, filedon returned HTTP 200 HTML, vidhide returned 403, and
  desustream returned 403 with `X-Frame-Options: SAMEORIGIN`. Results can differ
  on a phone, and the app surfaces failures instead of claiming success.

This is a property of the data source and its providers, not a missing Media3
feature. Actual playback was not verified on a physical device or emulator in
this environment.

## Fields AniStream does not invent

- Trending / recently added / recently updated categories: no endpoint exists.
  Home is composed from ongoing, completed and the weekly schedule.
- Year, type and sort filters: the API accepts none. Explore offers the
  supported status, genre and schedule browsing only.
- "Popular": there is no popularity endpoint. Completed titles are shown with
  the rating supplied by the API.
- Server/quality listing: no endpoint exists; qualities come from
  `episode.stream[].quality`.
