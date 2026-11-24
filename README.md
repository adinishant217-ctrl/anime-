# AniStream

A native Android anime catalogue and streaming application built with Kotlin,
Jetpack Compose, Material 3 and AndroidX Media3. It adapts the real data-source
architecture of [`shditz/anichi-cli`](https://github.com/shditz/anichi-cli) into a
proper Android app; it is **not** a WebView, website wrapper or terminal UI.

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

- Media3 ExoPlayer with HLS, DASH and progressive support.
- Play/pause, seek bar, previous/next episode, fullscreen/landscape, retry,
  buffering and error states.
- Audio and subtitle track selection from Media3 tracks when the source
  provides them.
- Quality switching across direct sources.
- Resume from the saved position, save every 5 seconds while playing, save on
  pause and on leaving the player.
- Autoplay next episode.

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
- Episode `stream` entries are **third-party embed pages** (filedon, vidhide,
  mega, desustream, Blogger). No direct media URL, DRM metadata or authorized
  stream is returned.
- AniStream therefore **does not extract or bypass embed players**. The source
  resolver only accepts direct `.mp4`, `.m3u8`, `.mpd`, `.mkv`, `.webm` or
  `.mov` URLs. For the current catalogue every episode is embed-only, so the
  player shows **“No native source available”** with the provider names and an
  explanation. It never pretends playback works and never uses fake URLs.
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
  player/engine/               Media3 ExoPlayer engine
  player/source/               direct-source resolver/quality ordering
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
2. The workflow installs JDK 17 and Android SDK 34, runs
   `./gradlew testDebugUnitTest`, then `./gradlew assembleDebug`.
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
- Playback source resolution and direct-URL detection.
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
metadata from the same API as the reference CLI and only offers direct,
compatible sources to Media3. It does not bypass DRM, authentication, embed
players or provider restrictions. The API's upstream source and its terms are
outside this repository; validate that you are authorized to use the catalogue
and any playback source in your region before distributing the app.

The reference project is MIT licensed and is credited as the source of the API
and data-model analysis.
