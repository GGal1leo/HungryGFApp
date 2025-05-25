package com.gal1leo.hungrygf

import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

interface PlacesApiService {
    @POST("v1/places:searchText")
    @Headers("Content-Type: application/json")
    suspend fun searchPlaces(
        @Header("X-Goog-Api-Key") apiKey: String,
        @Header("X-Goog-Fieldmask") fieldMask: String = "places.displayName,places.formattedAddress,places.priceLevel,places.rating,places.currentOpeningHours",
        @Query("textQuery") textQuery: String
    ): PlaceResponse
}

