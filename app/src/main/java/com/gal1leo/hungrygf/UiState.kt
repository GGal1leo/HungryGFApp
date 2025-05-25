package com.gal1leo.hungrygf

/**
 * Sealed class representing different UI states for the food search feature
 */
sealed class UiState {
    /**
     * Initial state when the app starts
     */
    object Idle : UiState()
    
    /**
     * Loading state when searching for places
     */
    data class Loading(val message: String = "Searching for restaurants...") : UiState()
    
    /**
     * Success state when a restaurant is found
     */
    data class Success(
        val restaurantName: String,
        val location: String
    ) : UiState()
    
    /**
     * Error state when something goes wrong
     */
    data class Error(
        val message: String,
        val errorType: ErrorType = ErrorType.GENERIC,
        val suggestions: List<String> = emptyList(),
        val isRetryable: Boolean = true,
        val needsNetworkCheck: Boolean = false,
        val actionButtons: List<ErrorAction> = emptyList()
    ) : UiState()
    
    /**
     * Empty result state when no restaurants are found
     */
    data class Empty(
        val searchLocation: String,
        val suggestions: List<String> = listOf(
            "Check spelling",
            "Use a more specific location", 
            "Try nearby city name"
        )
    ) : UiState()
    
    /**
     * Input validation error state
     */
    data class ValidationError(
        val message: String,
        val suggestions: List<String> = emptyList()
    ) : UiState()
}

/**
 * Specific error types for better categorization and handling
 */
enum class ErrorType {
    NETWORK_CONNECTION,    // No internet connection
    NETWORK_TIMEOUT,      // Request timed out
    NETWORK_SERVER,       // Server connection issues
    API_AUTHENTICATION,   // API key issues
    API_RATE_LIMIT,      // Too many requests
    API_QUOTA_EXCEEDED,  // API quota exceeded
    LOCATION_PERMISSION, // Location permission denied
    LOCATION_UNAVAILABLE, // GPS/location services issues
    VALIDATION,          // Input validation errors
    GENERIC             // Other/unknown errors
}

/**
 * Actionable buttons that can be shown with errors
 */
data class ErrorAction(
    val label: String,
    val actionType: ActionType,
    val isEnabled: Boolean = true
)

enum class ActionType {
    RETRY,
    RETRY_WITH_REFRESH,
    CHECK_CONNECTION,
    OPEN_SETTINGS,
    CONTACT_SUPPORT,
    TRY_DIFFERENT_LOCATION
}

/**
 * Network status information
 */
data class NetworkStatus(
    val isConnected: Boolean,
    val connectionType: ConnectionType = ConnectionType.UNKNOWN,
    val signalStrength: SignalStrength = SignalStrength.UNKNOWN
)

enum class ConnectionType {
    WIFI,
    MOBILE,
    ETHERNET,
    UNKNOWN
}

enum class SignalStrength {
    EXCELLENT,
    GOOD,
    FAIR,
    POOR,
    UNKNOWN
}
