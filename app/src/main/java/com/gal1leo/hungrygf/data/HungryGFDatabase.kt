package com.gal1leo.hungrygf.data

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room database for Hungry GF app
 * Stores search history, favorites, and cached results
 */
@Database(
    entities = [
        SearchHistoryEntity::class,
        FavoriteRestaurantEntity::class,
        CachedResultEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class HungryGFDatabase : RoomDatabase() {
    
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun favoriteRestaurantDao(): FavoriteRestaurantDao
    abstract fun cachedResultDao(): CachedResultDao
    
    companion object {
        const val DATABASE_NAME = "hungry_gf_database"
        
        // Migration from version 1 to 2 (example for future use)
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add migration logic here when needed
            }
        }
    }
}
