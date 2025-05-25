package com.gal1leo.hungrygf

import java.time.ZonedDateTime

class PlacesRepository(private val service: PlacesApiService) {
    private val priceLevels = mapOf(
        "PRICE_LEVEL_INEXPENSIVE" to 2,
        "PRICE_LEVEL_MODERATE" to 1,
        "PRICE_LEVEL_EXPENSIVE" to 0,
        "PRICE_LEVEL_VERY_EXPENSIVE" to 0
    )

    suspend fun findFoodPlaces(location: String, apiKey: String): String? {
        // Validate input parameters
        if (location.isBlank()) {
            throw IllegalArgumentException("Location cannot be blank")
        }
        if (apiKey.isBlank()) {
            throw IllegalArgumentException("API key cannot be blank")
        }
        
        val textQuery = "food in $location"
        println("$textQuery")
        
        return try {
            val response = service.searchPlaces(apiKey, textQuery = textQuery)
            println(response)
            
            // Ensure response is not null before processing
            response?.let { processPlaces(it) }
        } catch (e: Exception) {
            println("Error in findFoodPlaces: ${e.message}")
            throw e // Re-throw to let ViewModel handle it appropriately
        }
    }

    private fun processPlaces(response: PlaceResponse): String? {
        // Safely handle potentially empty places list
        val availablePlaces = response.places
            .filter { place ->
                // Ensure place has required data
                place.displayName.text.isNotBlank() && 
                (place.rating ?: 0.0) > 0.0
            }
            .filter { place ->
                val openingHours = place.currentOpeningHours ?: return@filter false
                isPlaceAvailable(openingHours)
            }
            .mapNotNull { place ->
                try {
                    // Safe access to potentially null priceLevel
                    val priceScore = priceLevels[place.priceLevel] ?: 0
                    PlaceInfo(
                        place,
                        place.rating ?: 0.0,
                        priceScore
                    )
                } catch (e: Exception) {
                    println("Error processing place ${place.displayName.text}: ${e.message}")
                    null // Skip this place if there's an error
                }
            }
            .sortedWith(compareByDescending<PlaceInfo> { it.rating }.thenByDescending { it.priceLevel })

        if (availablePlaces.isEmpty()) {
            println("No available places found after filtering")
            return null
        }

        return selectRandomPlace(availablePlaces)
    }
    
    private fun selectRandomPlace(places: List<PlaceInfo>): String? {
        if (places.isEmpty()) return null
        
        val weights = places.map { it.rating + it.priceLevel }
        val totalWeight = weights.sum()
        
        if (totalWeight <= 0.0) {
            // Fallback to first place if weights are invalid
            return places.firstOrNull()?.place?.displayName?.text
        }
        
        val random = (totalWeight * Math.random())
        var cumulative = 0.0

        for ((i, weight) in weights.withIndex()) {
            cumulative += weight
            if (random <= cumulative) {
                return places.getOrNull(i)?.place?.displayName?.text
            }
        }
        
        // Fallback to last place
        return places.lastOrNull()?.place?.displayName?.text
    }


    private fun isPlaceAvailable(openingHours: CurrentOpeningHours): Boolean {
        return try {
            // Check if place will be open soon (within 30 minutes)
            openingHours.nextOpenTime?.let { nextOpenStr ->
                if (nextOpenStr.isBlank()) return@let null
                
                val nextOpen = ZonedDateTime.parse(nextOpenStr)
                val now = ZonedDateTime.now(nextOpen.zone)
                println("Next open: $nextOpen, Now: $now")
                
                // Place is available if it opens within 30 minutes
                return !nextOpen.isBefore(now.plusMinutes(30))
            } ?: run {
                // If no nextOpenTime, check if it will close soon
                openingHours.nextCloseTime?.let { nextCloseStr ->
                    if (nextCloseStr.isBlank()) return@let null
                    
                    val nextClose = ZonedDateTime.parse(nextCloseStr)
                    val now = ZonedDateTime.now(nextClose.zone)
                    println("Next close: $nextClose, Now: $now")
                    
                    // Place is available if it closes after 1.5 hours from now
                    return nextClose.isAfter(now.plusHours(1).plusMinutes(30))
                } ?: true // If no timing info, assume available
            }
        } catch (e: Exception) {
            println("Error parsing opening hours: ${e.message}")
            // If we can't parse the times, assume the place is available
            // This is safer than excluding potentially good restaurants
            true
        }
    }

    private data class PlaceInfo(
        val place: Place,
        val rating: Double,
        val priceLevel: Int
    )
}
