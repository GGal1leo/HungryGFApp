package com.gal1leo.hungrygf.repository

import com.gal1leo.hungrygf.BuildConfig
import com.gal1leo.hungrygf.PlaceResponse
import com.gal1leo.hungrygf.PlacesApiService
import com.gal1leo.hungrygf.data.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

/**
 * Enhanced repository with dependency injection, caching, and error handling
 */
class EnhancedPlacesRepository(
    private val apiService: PlacesApiService,
    private val searchHistoryDao: SearchHistoryDao,
    private val favoriteDao: FavoriteRestaurantDao,
    private val cachedResultDao: CachedResultDao
) {
    
    private val priceLevels = mapOf(
        "PRICE_LEVEL_INEXPENSIVE" to 2,
        "PRICE_LEVEL_MODERATE" to 1,
        "PRICE_LEVEL_EXPENSIVE" to 0,
        "PRICE_LEVEL_VERY_EXPENSIVE" to 0
    )

    /**
     * Find food places with caching and error handling
     */
    suspend fun findFoodPlaces(
        location: String,
        forceRefresh: Boolean = false
    ): Result<RestaurantResult> {
        return try {
            // Validate input
            if (location.isBlank()) {
                return Result.failure(IllegalArgumentException("Location cannot be blank"))
            }

            // Check cache first (unless force refresh)
            if (!forceRefresh) {
                val cachedResult = cachedResultDao.getCachedResult(location)
                if (cachedResult != null) {
                    return Result.success(
                        RestaurantResult(
                            name = cachedResult.restaurantName,
                            location = location,
                            placeId = cachedResult.placeId,
                            rating = cachedResult.rating,
                            priceLevel = cachedResult.priceLevel,
                            photoUrl = cachedResult.photoUrl,
                            isFromCache = true
                        )
                    )
                }
            }

            // Make API call
                    val textQuery = "food in $location"
            val response = apiService.searchPlaces(BuildConfig.GOOGLE_PLACES_API_KEY, textQuery = textQuery)
            
            processPlacesResponse(response, location)
                
        } catch (e: Exception) {
            // Save failed search to history
            saveSearchHistory(location, "", isSuccessful = false)
            Result.failure(e)
        }
    }

    /**
     * Process API response and save to cache/history
     */
    private suspend fun processPlacesResponse(
        response: PlaceResponse,
        location: String
    ): Result<RestaurantResult> {
        val availablePlaces = response.places
            .filter { place ->
                place.displayName.text.isNotBlank() && (place.rating ?: 0.0) > 0.0
            }
            .filter { place ->
                val openingHours = place.currentOpeningHours ?: return@filter false
                isPlaceAvailable(openingHours)
            }
            .mapNotNull { place ->
                try {
                    val priceLevel = place.priceLevel?.let { level ->
                        priceLevels[level] ?: 1
                    } ?: 1

                    PlaceWithScore(
                        place = place,
                        score = calculatePlaceScore(place.rating ?: 0.0, priceLevel)
                    )
                } catch (e: Exception) {
                    null
                }
            }

        if (availablePlaces.isEmpty()) {
            saveSearchHistory(location, "", isSuccessful = false)
            return Result.failure(Exception("No restaurants found for '$location'"))
        }

        // Select place using weighted random selection for variety
        val selectedPlace = selectRandomPlace(availablePlaces)?.place
            ?: return Result.failure(Exception("No suitable restaurant found"))

        println("Selected restaurant: ${selectedPlace.displayName.text} from ${availablePlaces.size} available places")

        val result = RestaurantResult(
            name = selectedPlace.displayName.text,
            location = location,
            placeId = selectedPlace.id,
            rating = selectedPlace.rating?.toFloat(),
            priceLevel = selectedPlace.priceLevel?.let { priceLevels[it] },
            photoUrl = selectedPlace.photos?.firstOrNull()?.name,
            isFromCache = false
        )

        // Save to cache and history
        cacheResult(result)
        saveSearchHistory(location, result.name, isSuccessful = true)

        return Result.success(result)
    }

    /**
     * Save search result to cache
     */
    private suspend fun cacheResult(result: RestaurantResult) {
        val cacheEntity = CachedResultEntity(
            location = result.location,
            restaurantName = result.name,
            placeId = result.placeId,
            rating = result.rating,
            priceLevel = result.priceLevel,
            photoUrl = result.photoUrl
        )
        cachedResultDao.cacheResult(cacheEntity)
    }

    /**
     * Save search to history
     */
    private suspend fun saveSearchHistory(location: String, restaurantName: String, isSuccessful: Boolean) {
        val historyEntity = SearchHistoryEntity(
            location = location,
            restaurantName = restaurantName,
            isSuccessful = isSuccessful
        )
        searchHistoryDao.insertSearch(historyEntity)
    }

    /**
     * Get search history
     */
    fun getSearchHistory(): Flow<List<SearchHistoryEntity>> {
        return searchHistoryDao.getRecentSearches()
    }

    /**
     * Get successful searches for suggestions
     */
    fun getSuccessfulSearches(): Flow<List<SearchHistoryEntity>> {
        return searchHistoryDao.getSuccessfulSearches()
    }

    /**
     * Toggle favorite status
     */
    suspend fun toggleFavorite(restaurantName: String, location: String): Boolean {
        return if (favoriteDao.isFavorite(restaurantName, location)) {
            favoriteDao.removeFavorite(restaurantName, location)
            false
        } else {
            val favorite = FavoriteRestaurantEntity(
                name = restaurantName,
                location = location
            )
            favoriteDao.addFavorite(favorite)
            true
        }
    }

    /**
     * Check if restaurant is favorite
     */
    suspend fun isFavorite(restaurantName: String, location: String): Boolean {
        return favoriteDao.isFavorite(restaurantName, location)
    }

    /**
     * Get all favorites
     */
    fun getFavorites(): Flow<List<FavoriteRestaurantEntity>> {
        return favoriteDao.getAllFavorites()
    }

    /**
     * Clear expired cache
     */
    suspend fun clearExpiredCache() {
        cachedResultDao.cleanExpiredCache()
    }

    /**
     * Clear search history older than 30 days
     */
    suspend fun clearOldHistory() {
        val cutoffTime = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L) // 30 days
        searchHistoryDao.deleteOldSearches(cutoffTime)
    }

    // Helper functions from original repository
    private fun isPlaceAvailable(openingHours: com.gal1leo.hungrygf.CurrentOpeningHours?): Boolean {
        return openingHours?.openNow == true
    }

    private fun calculatePlaceScore(rating: Double, priceLevel: Int): Double {
        val ratingWeight = 0.7
        val priceLevelWeight = 0.3
        val maxRating = 5.0
        val maxPriceLevel = 3.0

        val normalizedRating = rating / maxRating
        val normalizedPriceLevel = priceLevel / maxPriceLevel

        return (normalizedRating * ratingWeight) + (normalizedPriceLevel * priceLevelWeight)
    }

    /**
     * Select a random place using weighted selection based on scores
     * This ensures variety in restaurant suggestions
     */
    private fun selectRandomPlace(places: List<PlaceWithScore>): PlaceWithScore? {
        if (places.isEmpty()) return null
        
        val weights = places.map { it.score }
        val totalWeight = weights.sum()
        
        if (totalWeight <= 0.0) {
            // Fallback to random selection if scores are invalid
            return places.random()
        }
        
        val random = totalWeight * kotlin.random.Random.nextDouble()
        var cumulative = 0.0

        for ((index, weight) in weights.withIndex()) {
            cumulative += weight
            if (random <= cumulative) {
                return places[index]
            }
        }
        
        // Fallback to last place if we didn't select one (shouldn't happen)
        return places.lastOrNull()
    }
}

/**
 * Data classes for enhanced repository
 */
data class RestaurantResult(
    val name: String,
    val location: String,
    val placeId: String? = null,
    val rating: Float? = null,
    val priceLevel: Int? = null,
    val photoUrl: String? = null,
    val isFromCache: Boolean = false
)

private data class PlaceWithScore(
    val place: com.gal1leo.hungrygf.Place,
    val score: Double
)
