package com.anistream.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.anistream.app.data.local.dao.WatchHistoryDao
import com.anistream.app.data.local.dao.WatchlistDao
import com.anistream.app.data.local.entity.WatchHistoryEntity
import com.anistream.app.data.local.entity.WatchlistEntity

@Database(
    entities = [WatchHistoryEntity::class, WatchlistEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun watchHistoryDao(): WatchHistoryDao
    abstract fun watchlistDao(): WatchlistDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "anistream.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}
