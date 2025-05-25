package com.gal1leo.hungrygf

data class TownResponse(
    val places: List<Town> = emptyList()
)

data class Town(
    val displayName: DisplayName,
    val addressComponents: List<AddressComponent>?,
)

data class AddressComponent(
    val longText: String,
    val shortText: String,
    val types: List<String>,
    val languageCode: String
)

