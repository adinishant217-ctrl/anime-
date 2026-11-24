package com.anistream.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "watch_history",
    primaryKeys = ["animeSlug", "episodeNumber"],
    indices = [Index("updatedAt")]
)
data class WatchHistoryEntity(
    val animeSlug: String,
    val episodeNumber: Int,
    val animeTitle: String,
    val coverUrl: String?,
    val episodeSlug: String,
    val episodeTitle: String,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val completed: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
