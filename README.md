# AniStream

A native Android anime catalogue and streaming application built with Kotlin,
Jetpack Compose, Material 3 and AndroidX Media3. It adapts the real data-source
architecture of [`shditz/anichi-cli`](https://github.com/shditz/anichi-cli) into a
proper Android app; it is **not** a website wrapper or terminal UI. The only
WebView use is the provider-page fallback described below, where the provider's
own player is shown because it does not expose a direct media URL.

- **Package:** `com.anistream.app`
- **App name:** AniStream
- **Minimum Android:** API 24
- **Compile / target SDK:** 34
- **Build system:** Gradle 8.7, AGP 8.5.2, Kotlin 1.9.24
- **CI:** `.github/workflows/android-build.yml`

## What is implemented

### Catalogue (real data, no demo titles)

- Home: featured banner, ongoing, completed and continue-watching rows.
- Explore: ongoing / completed / weekly schedule, genre filters, pagination.
- Search: live API search, 400 ms debounce, recent-search history, loading,
  empty and error states.
- Anime details: poster, title, Japanese title, synopsis, genres, type, status,
  episode count, duration, studio, producers, score and release date *when the
  API supplies them*.
- Episodes: real episode slugs and titles, parsed episode numbers, release dates,
  watched indicators and resume progress.
- Weekly schedule grouped by the API's Indonesian day keys.

### Library and resume

- Room database for watchlist and watch history.
- Per-episode playback position and duration, completion at 90%.
- Continue-watching row appears only for episodes with real playback progress.
- History and watchlist remain available offline.
- DataStore settings: Midnight / Dark / AMOLED theme, autoplay, preferred
  quality, search history.

### Player

- Dedicated `PlaybackResolver` classifies every catalogue source as direct
  media, a provider player page, or unavailable. It does not scrape player
  pages or derive media URLs from their JavaScript.
- Native Media3 ExoPlayer path for direct HLS (`.m3u8`), DASH (`.mpd`) and
  progressive (`.mp4`, `.m4v`, `.mkv`, `.webm`, `.mov`) sources.
- Native requests carry the episode-page `Referer` and a browser-compatible
  `User-Agent`; redirects, connect/read timeouts and cancelled loads are handled
  by Media3. A playback error re-resolves an expired temporary source once.
- Play/pause, seek bar, previous/next episode, fullscreen/landscape, retry,
  buffering and error states.
- Audio and subtitle track selection from Media3 tracks when the source
  provides them; in-manifest subtitles/audio are reported by Media3.
- Quality switching across direct sources.
- Resume from the saved position, save every 5 seconds while playing, save on
  pause and on leaving the player, and autoplay the next native episode.
- **Authorized provider-page fallback:** when the catalogue returns a player
  page (all current providers do), the app loads that page top-level in a
  WebView with provider selection, fullscreen, loading/error states, and an
  “Open in browser” fallback. The provider keeps control of its player. No
  DRM, authentication, anti-bot protection, geo-restriction or access control is
  bypassed.
- Provider pages that reject embedded playback are reported honestly; the user
  can choose another provider or open the page in their browser. AniStream never
  pretends a failed provider page is a playable direct video.

### UI

Dark anime-themed Material 3 design with three palettes (Midnight, Dark, AMOLED),
poster cards, a large featured banner, responsive grids, skeleton loading,
empty/error states, Coil image caching and gradient fallbacks.

Screens: Home, Explore, Search, Anime Details, Episode List, Player, Library
(Watchlist + History), Profile, Settings.

## Verified API and hard limitations

Full details, payload samples and observed quirks are in
[`docs/API.md`](docs/API.md). The important limits:

- The API is **ShivraAPI** (`https://shivraapi.my.id/otd`), upstream Otakudesu.
- `GET /otd` returns **404**; Home is composed from `/ongoing`, `/completed` and
  `/schedule`.
- There is **no** year, type, sort, trending, recently-added or "all anime"
  endpoint, and search has no pagination.
- There is **no** server/quality endpoint, audio list, language metadata or
  subtitle list.
- Episode `stream` entries are **third-party player pages** (filedon, vidhide,
  mega, desustream, Blogger). No direct media URL, DRM metadata, subtitle list
  or authorized stream is returned.
- The original CLI's relevant path is the browser fallback: `index.ts` opens any
  URL containing `/embed/` or `player` in the desktop browser and only attempts
  MPV/yt-dlp for non-embed URLs. Android's equivalent is the provider WebView
  fallback, not desktop yt-dlp extraction.
- AniStream **does not extract or bypass player pages**. Direct URLs are played
  natively; recognized player pages are loaded in the provider's own WebView
  player; anything else is unavailable. There are no hardcoded episode URLs and
  no fake media URLs.
- Provider availability is not guaranteed. In a CI-server check on 24 Nov 2026,
  filedon responded with its HTML player (HTTP 200), while vidhide returned 403
  and desustream returned 403 with `X-Frame-Options: SAMEORIGIN`. A phone on a
  normal network may get different results. WebView failures are surfaced and
  the browser fallback is offered instead of claiming playback succeeded.
- Poster `cover` is the only artwork; there is no banner. No field is invented
  when the API omits it.

The player layer is independent of the catalogue: if a direct authorized source
appears, the same Media3 player uses it without catalogue-specific changes.

## Project structure

```
app/src/main/java/com/anistream/app/
  AniStreamApplication.kt, MainActivity.kt
  core/common/                 error mapping, episode-number parsing
  data/remote/                 Retrofit API, DTOs, tolerant JSON parsing, mappers
  data/local/                  Room database, entities, DAOs
  data/repository/             catalogue, library, settings implementations
  domain/model/                domain models
  domain/repository/           repository contracts
  player/engine/               Media3 ExoPlayer engine + request headers
  player/source/               native/provider-page resolver and quality order
  player/state/                playback and track state
  ui/navigation/               Compose Navigation + bottom bar
  ui/screen/                   home, explore, search, details, episodes,
                               player, library, profile, settings
  ui/component/                poster cards, rows, states, skeletons
  ui/theme/                    Material 3 palettes and typography
  di/AppContainer.kt           manual dependency injection
app/src/test/                  JVM unit tests + Robolectric Room tests
docs/API.md                    verified endpoint reference and limitations
```

Architecture is clean MVVM: UI → ViewModels → domain repositories → remote and
Room data sources, with the playback engine and source discovery isolated from
the catalogue.

## Build only through GitHub Actions

The project is intentionally **not built locally**; the workflow compiles and
packages it on an Ubuntu runner.

1. Push to `main`, `develop` or a `freebuff/**` branch, or run **Android Build →
   Run workflow** manually.
2. The workflow uses the runner's preinstalled Android SDK, installs platform
   34 if needed, runs `./gradlew testDebugUnitTest`, then
   `./gradlew assembleDebug`. The previous `android-actions/setup-android` step
   was removed because it failed while looking for a package named `tools`.
3. Download **AniStream-debug-apk** from the run's *Artifacts* section.

The workflow also uploads test reports when a step fails and writes a summary
with the commit and artifact name.

> Release signing is not configured. The workflow produces a debug APK, as
> required. Add signing secrets and a release step only when credentials exist;
> never commit keystores or passwords.

## Tests

Run remotely by the workflow:

- API envelope parsing and DTO → domain mapping (ongoing, detail, episode,
  search, schedule, genres, error envelope).
- Episode-number extraction.
- Playback source resolution: direct media detection, HLS/DASH MIME types,
  provider-page classification, required headers, quality ordering and
  unavailable sources.
- Player states: native preparation, provider-page fallback, provider errors and
  one automatic re-resolve after a native playback error.
- Catalogue repository delegation.
- Library repository (progress, completion, resume preservation, watchlist).
- Room DAOs under Robolectric (history, resume, completion, watchlist).
- ViewModel state for Home, Search, Details and Player (available/unavailable
  source paths).

```bash
./gradlew testDebugUnitTest        # run by CI, not required locally
./gradlew assembleDebug            # run by CI, not required locally
```

## Data source and authorization

AniStream does not host, re-encode or proxy video. It reads public catalogue
metadata from the same API as the reference CLI. Direct media is played by
Media3; a provider's own player page may be loaded in a WebView. It does not
scrape, extract or bypass DRM, authentication, anti-bot protections, geo
restrictions or provider access controls. The API's upstream source and its
terms are outside this repository; validate that you are authorized to use the
catalogue and any playback source in your region before distributing the app.

Embedded playback was implemented and compiled through CI, but it has not been
verified on a physical Android device or emulator in this environment. Provider
availability, WebView compatibility and region-specific access must be checked
on the target device.

The reference project is MIT licensed and is credited as the source of the API
and data-model analysis.
