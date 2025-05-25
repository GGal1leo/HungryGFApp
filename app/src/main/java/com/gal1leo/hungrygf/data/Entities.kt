package com.gal1leo.hungrygf.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for storing restaurant search history
 */
@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val location: String,
    val restaurantName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSuccessful: Boolean = true
)

/**
 * Room entity for storing favorite restaurants
 */
@Entity(tableName = "favorites")
data class FavoriteRestaurantEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val location: String,
    val placeId: String? = null,
    val rating: Float? = null,
    val priceLevel: Int? = null,
    val photoUrl: String? = null,
    val addedAt: Long = System.currentTimeMillis()
)

/**
 * Room entity for caching recent search results
 */
@Entity(tableName = "cached_results")
data class CachedResultEntity(
    @PrimaryKey
    val location: String,
    val restaurantName: String,
    val placeId: String? = null,
    val rating: Float? = null,
    val priceLevel: Int? = null,
    val photoUrl: String? = null,
    val cachedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + (24 * 60 * 60 * 1000) // 24 hours
)
