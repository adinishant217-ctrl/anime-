package com.anistream.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.anistream.app.data.local.entity.WatchHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchHistoryDao {

    @Query("SELECT * FROM watch_history ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<WatchHistoryEntity>>

    @Query(
        "SELECT * FROM watch_history WHERE positionMs > 0 AND completed = 0 " +
            "ORDER BY updatedAt DESC LIMIT 20"
    )
    fun observeContinueWatching(): Flow<List<WatchHistoryEntity>>

    @Query("SELECT * FROM watch_history WHERE animeSlug = :slug ORDER BY episodeNumber ASC")
    fun observeForAnime(slug: String): Flow<List<WatchHistoryEntity>>

    @Query("SELECT * FROM watch_history WHERE animeSlug = :slug AND episodeNumber = :number LIMIT 1")
    suspend fun find(slug: String, number: Int): WatchHistoryEntity?

    @Query(
        "SELECT * FROM watch_history WHERE animeSlug = :slug AND positionMs > 0 AND completed = 0 " +
            "ORDER BY updatedAt DESC LIMIT 1"
    )
    suspend fun latestResumeForAnime(slug: String): WatchHistoryEntity?

    @Upsert
    suspend fun upsert(entity: WatchHistoryEntity)

    @Query("DELETE FROM watch_history WHERE animeSlug = :slug AND episodeNumber = :number")
    suspend fun deleteEpisode(slug: String, number: Int)

    @Query("DELETE FROM watch_history WHERE animeSlug = :slug")
    suspend fun deleteForAnime(slug: String)

    @Query("DELETE FROM watch_history")
    suspend fun clear()
}
