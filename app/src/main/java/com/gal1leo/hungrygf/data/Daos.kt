package com.gal1leo.hungrygf.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO for search history operations
 */
@Dao
interface SearchHistoryDao {
    
    @Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT 20")
    fun getRecentSearches(): Flow<List<SearchHistoryEntity>>
    
    @Query("SELECT * FROM search_history WHERE isSuccessful = 1 ORDER BY timestamp DESC LIMIT 10")
    fun getSuccessfulSearches(): Flow<List<SearchHistoryEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearch(search: SearchHistoryEntity)
    
    @Query("DELETE FROM search_history WHERE timestamp < :cutoffTime")
    suspend fun deleteOldSearches(cutoffTime: Long)
    
    @Query("DELETE FROM search_history")
    suspend fun clearAllHistory()
}

/**
 * DAO for favorite restaurants operations
 */
@Dao
interface FavoriteRestaurantDao {
    
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteRestaurantEntity>>
    
    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE name = :restaurantName AND location = :location)")
    suspend fun isFavorite(restaurantName: String, location: String): Boolean
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteRestaurantEntity)
    
    @Query("DELETE FROM favorites WHERE name = :restaurantName AND location = :location")
    suspend fun removeFavorite(restaurantName: String, location: String)
    
    @Delete
    suspend fun deleteFavorite(favorite: FavoriteRestaurantEntity)
}

/**
 * DAO for cached results operations
 */
@Dao
interface CachedResultDao {
    
    @Query("SELECT * FROM cached_results WHERE location = :location AND expiresAt > :currentTime LIMIT 1")
    suspend fun getCachedResult(location: String, currentTime: Long = System.currentTimeMillis()): CachedResultEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun cacheResult(result: CachedResultEntity)
    
    @Query("DELETE FROM cached_results WHERE expiresAt < :currentTime")
    suspend fun cleanExpiredCache(currentTime: Long = System.currentTimeMillis())
    
    @Query("DELETE FROM cached_results")
    suspend fun clearAllCache()
}
