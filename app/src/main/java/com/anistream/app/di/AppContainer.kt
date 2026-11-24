package com.anistream.app.di

import android.content.Context
import com.anistream.app.data.local.AppDatabase
import com.anistream.app.data.remote.AnimeRemoteDataSource
import com.anistream.app.data.remote.ShivraAnimeRemoteDataSource
import com.anistream.app.data.repository.AnimeRepositoryImpl
import com.anistream.app.data.repository.LibraryRepositoryImpl
import com.anistream.app.data.repository.SettingsRepositoryImpl
import com.anistream.app.domain.repository.AnimeRepository
import com.anistream.app.domain.repository.LibraryRepository
import com.anistream.app.domain.repository.SettingsRepository
import com.anistream.app.player.engine.Media3PlaybackEngine
import com.anistream.app.player.engine.PlaybackEngine
import com.anistream.app.player.source.PlaybackResolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    /** Used for progress writes that must finish after a ViewModel is cleared. */
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val database: AppDatabase by lazy { AppDatabase.build(appContext) }

    private val remoteDataSource: AnimeRemoteDataSource by lazy {
        ShivraAnimeRemoteDataSource(appContext)
    }

    val animeRepository: AnimeRepository by lazy { AnimeRepositoryImpl(remoteDataSource) }

    val libraryRepository: LibraryRepository by lazy {
        LibraryRepositoryImpl(database.watchHistoryDao(), database.watchlistDao())
    }

    val settingsRepository: SettingsRepository by lazy { SettingsRepositoryImpl(appContext) }

    fun createPlaybackEngine(): PlaybackEngine = Media3PlaybackEngine(appContext)

    fun createPlaybackResolver(): PlaybackResolver = PlaybackResolver()
}
