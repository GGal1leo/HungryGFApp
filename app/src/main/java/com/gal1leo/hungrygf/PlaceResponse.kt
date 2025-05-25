package com.gal1leo.hungrygf

data class PlaceResponse(
    val places: List<Place> = emptyList()
)

data class Place(
    val id: String,
    val displayName: DisplayName,
    val formattedAddress: String,
    val priceLevel: String?,
    val rating: Double?,
    val currentOpeningHours: CurrentOpeningHours?,
    val photos: List<Photo>?
)

data class Photo(
    val name: String
)

data class DisplayName(
    val text: String
)

data class CurrentOpeningHours(
    val openNow: Boolean?,
    val nextOpenTime: String?,
    val nextCloseTime: String?
)
