package com.anistream.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watchlist")
data class WatchlistEntity(
    @PrimaryKey val slug: String,
    val title: String,
    val coverUrl: String?,
    val status: String?,
    val type: String?,
    val addedAt: Long = System.currentTimeMillis()
)
