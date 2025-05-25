package com.gal1leo.hungrygf

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gal1leo.hungrygf.repository.EnhancedPlacesRepository
import com.gal1leo.hungrygf.di.DependencyContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlacesViewModel(
    private val repository: EnhancedPlacesRepository = DependencyContainer.getRepository()
) : ViewModel() {
    
    // StateFlow for UI state management
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    // Network status for enhanced error handling
    private var networkStatusManager: NetworkStatusManager? = null

    /**
     * Search for food places and update UI state accordingly
     */
    fun searchFoodPlace(location: String, forceRefresh: Boolean = false) {
        // Validate input first
        when (val validationResult = InputValidator.validateLocation(location)) {
            is ValidationResult.Error -> {
                val suggestions = InputValidator.getSuggestions(location)
                _uiState.value = UiState.ValidationError(
                    message = validationResult.message,
                    suggestions = suggestions
                )
                return
            }
            is ValidationResult.Success -> {
                val validLocation = validationResult.validInput
                
                // Additional reasonableness check
                if (!InputValidator.isReasonableLocation(validLocation)) {
                    val suggestions = InputValidator.getSuggestions(location)
                    _uiState.value = UiState.ValidationError(
                        message = "This doesn't look like a valid location",
                        suggestions = suggestions
                    )
                    return
                }
                
                // Proceed with search
                performSearch(validLocation, forceRefresh)
            }
        }
    }
    
    private fun performSearch(location: String, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading("Searching for restaurants in $location...")
                
                val result = repository.findFoodPlaces(location, forceRefresh)
                     result.fold(
                onSuccess = { restaurantResult ->
                    if (restaurantResult.name.isNotBlank()) {
                        _uiState.value = UiState.Success(
                            restaurantName = restaurantResult.name,
                            location = location
                        )
                    } else {
                        _uiState.value = UiState.Empty(searchLocation = location)
                    }
                },
                onFailure = { exception ->
                    val errorInfo = categorizeError(exception)
                    _uiState.value = UiState.Error(
                        message = errorInfo.message,
                        errorType = errorInfo.type,
                        suggestions = errorInfo.suggestions,
                        isRetryable = errorInfo.isRetryable,
                        needsNetworkCheck = errorInfo.needsNetworkCheck,
                        actionButtons = errorInfo.actionButtons
                    )
                }
            )
                
            } catch (e: IllegalArgumentException) {
                _uiState.value = UiState.ValidationError(
                    message = e.localizedMessage ?: "Invalid input",
                    suggestions = InputValidator.getSuggestions(location)
                )
            } catch (e: Exception) {
                val errorInfo = categorizeError(e)
                _uiState.value = UiState.Error(
                    message = errorInfo.message,
                    errorType = errorInfo.type,
                    suggestions = errorInfo.suggestions,
                    isRetryable = errorInfo.isRetryable,
                    needsNetworkCheck = errorInfo.needsNetworkCheck,
                    actionButtons = errorInfo.actionButtons
                )
            }
        }
    }
    
    /**
     * Initialize network status manager
     */
    fun initializeNetworkManager(context: android.content.Context) {
        networkStatusManager = NetworkStatusManager(context)
    }
    
    /**
     * Clean up resources
     */
    override fun onCleared() {
        super.onCleared()
        networkStatusManager?.unregisterNetworkCallback()
    }

    /**
     * Reset UI state to idle
     */
    fun resetState() {
        _uiState.value = UiState.Idle
    }

    /**
     * Toggle favorite status for a restaurant
     */
    fun toggleFavorite(restaurantName: String, location: String) {
        viewModelScope.launch {
            repository.toggleFavorite(restaurantName, location)
        }
    }

    /**
     * Check if restaurant is favorite
     */
    suspend fun isFavorite(restaurantName: String, location: String): Boolean {
        return repository.isFavorite(restaurantName, location)
    }

    /**
     * Get search history
     */
    fun getSearchHistory() = repository.getSearchHistory()

    /**
     * Get favorite restaurants
     */
    fun getFavorites() = repository.getFavorites()
    
    /**
     * Enhanced error categorization with specific error types and actionable suggestions
     */
    private fun categorizeError(exception: Throwable): ErrorInfo {
        val errorMessage = exception.message ?: "Unknown error"
        
        return when {
            // Network Connection Errors
            errorMessage.contains("UnknownHostException", ignoreCase = true) ||
            errorMessage.contains("No address associated with hostname", ignoreCase = true) -> {
                ErrorInfo(
                    message = "No internet connection detected",
                    type = ErrorType.NETWORK_CONNECTION,
                    suggestions = getNetworkSuggestions(),
                    isRetryable = true,
                    needsNetworkCheck = true,
                    actionButtons = listOf(
                        ErrorAction("Retry", ActionType.RETRY),
                        ErrorAction("Check Connection", ActionType.CHECK_CONNECTION)
                    )
                )
            }
            
            // Timeout Errors
            errorMessage.contains("SocketTimeoutException", ignoreCase = true) ||
            errorMessage.contains("timeout", ignoreCase = true) -> {
                ErrorInfo(
                    message = "Request timed out - please try again",
                    type = ErrorType.NETWORK_TIMEOUT,
                    suggestions = listOf(
                        "Check your internet connection speed",
                        "Try again in a few moments",
                        "Move to an area with better signal"
                    ),
                    isRetryable = true,
                    needsNetworkCheck = true,
                    actionButtons = listOf(
                        ErrorAction("Retry", ActionType.RETRY),
                        ErrorAction("Retry with Refresh", ActionType.RETRY_WITH_REFRESH)
                    )
                )
            }
            
            // Server Connection Errors
            errorMessage.contains("ConnectException", ignoreCase = true) ||
            errorMessage.contains("Connection refused", ignoreCase = true) -> {
                ErrorInfo(
                    message = "Cannot connect to server",
                    type = ErrorType.NETWORK_SERVER,
                    suggestions = listOf(
                        "Server may be temporarily unavailable",
                        "Check your internet connection",
                        "Try again in a few minutes"
                    ),
                    isRetryable = true,
                    needsNetworkCheck = false,
                    actionButtons = listOf(
                        ErrorAction("Retry", ActionType.RETRY),
                        ErrorAction("Contact Support", ActionType.CONTACT_SUPPORT)
                    )
                )
            }
            
            // API Authentication Errors
            errorMessage.contains("HTTP 401", ignoreCase = true) ||
            errorMessage.contains("Unauthorized", ignoreCase = true) -> {
                ErrorInfo(
                    message = "API authentication failed",
                    type = ErrorType.API_AUTHENTICATION,
                    suggestions = listOf(
                        "API key may be invalid or expired",
                        "Please contact support if this continues"
                    ),
                    isRetryable = false,
                    needsNetworkCheck = false,
                    actionButtons = listOf(
                        ErrorAction("Contact Support", ActionType.CONTACT_SUPPORT)
                    )
                )
            }
            
            // API Rate Limiting
            errorMessage.contains("HTTP 429", ignoreCase = true) ||
            errorMessage.contains("Too Many Requests", ignoreCase = true) -> {
                ErrorInfo(
                    message = "Too many requests - please wait a moment",
                    type = ErrorType.API_RATE_LIMIT,
                    suggestions = listOf(
                        "Please wait a minute before trying again",
                        "The service is busy right now"
                    ),
                    isRetryable = true,
                    needsNetworkCheck = false,
                    actionButtons = listOf(
                        ErrorAction("Retry in 1 minute", ActionType.RETRY)
                    )
                )
            }
            
            // API Quota Exceeded
            errorMessage.contains("HTTP 403", ignoreCase = true) ||
            errorMessage.contains("quota", ignoreCase = true) -> {
                ErrorInfo(
                    message = "Daily API quota exceeded",
                    type = ErrorType.API_QUOTA_EXCEEDED,
                    suggestions = listOf(
                        "Daily limit reached",
                        "Please try again tomorrow"
                    ),
                    isRetryable = false,
                    needsNetworkCheck = false,
                    actionButtons = listOf(
                        ErrorAction("Contact Support", ActionType.CONTACT_SUPPORT)
                    )
                )
            }
            
            // Location Permission Errors
            errorMessage.contains("permission", ignoreCase = true) ||
            errorMessage.contains("location", ignoreCase = true) -> {
                ErrorInfo(
                    message = "Location permission required",
                    type = ErrorType.LOCATION_PERMISSION,
                    suggestions = listOf(
                        "Grant location permission in settings",
                        "Or enter your location manually"
                    ),
                    isRetryable = false,
                    needsNetworkCheck = false,
                    actionButtons = listOf(
                        ErrorAction("Open Settings", ActionType.OPEN_SETTINGS),
                        ErrorAction("Try Different Location", ActionType.TRY_DIFFERENT_LOCATION)
                    )
                )
            }
            
            // Generic Network Error
            else -> {
                ErrorInfo(
                    message = "Network error occurred",
                    type = ErrorType.GENERIC,
                    suggestions = listOf(
                        "Check your internet connection",
                        "Try again in a few moments",
                        "Contact support if this continues"
                    ),
                    isRetryable = true,
                    needsNetworkCheck = true,
                    actionButtons = listOf(
                        ErrorAction("Retry", ActionType.RETRY),
                        ErrorAction("Check Connection", ActionType.CHECK_CONNECTION)
                    )
                )
            }
        }
    }
    
    /**
     * Get network-specific suggestions based on current network status
     */
    private fun getNetworkSuggestions(): List<String> {
        // Since we can't pass context here, return default network suggestions
        // The MainActivity will handle network-specific suggestions via NetworkStatusManager
        return listOf(
            "Check your internet connection",
            "Try connecting to WiFi",
            "Move to an area with better signal",
            "Try again in a few moments"
        )
    }
    
    /**
     * Converts technical errors to user-friendly messages (legacy method for compatibility)
     */
    private fun getErrorMessage(exception: Throwable): String {
        return categorizeError(exception).message
    }
}

/**
 * Data class to hold comprehensive error information
 */
data class ErrorInfo(
    val message: String,
    val type: ErrorType,
    val suggestions: List<String>,
    val isRetryable: Boolean,
    val needsNetworkCheck: Boolean,
    val actionButtons: List<ErrorAction>
)
