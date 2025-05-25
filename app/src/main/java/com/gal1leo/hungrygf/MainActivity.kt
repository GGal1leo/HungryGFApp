package com.gal1leo.hungrygf

import android.content.Intent
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import kotlin.coroutines.suspendCoroutine
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.coroutines.resume

class MainActivity : AppCompatActivity() {
    // UI Components
    private lateinit var locationInputLayout: TextInputLayout
    private lateinit var locationInput: TextInputEditText
    private lateinit var searchButton: MaterialButton
    private lateinit var locateMeButton: MaterialButton
    private lateinit var outputText: TextView
    private lateinit var progressBar: CircularProgressIndicator
    private lateinit var loadingCard: MaterialCardView
    private lateinit var resultCard: MaterialCardView
    private lateinit var loadingText: TextView
    private lateinit var resultIcon: ImageView
    private lateinit var favoriteIcon: ImageView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var fabHistory: com.google.android.material.floatingactionbutton.FloatingActionButton
    
    // Network status components
    private var networkStatusIndicator: TextView? = null
    private var networkStatusIcon: ImageView? = null
    private var networkStatusManager: NetworkStatusManager? = null
    
    // Location services
    private lateinit var locationClient: FusedLocationProviderClient
    private val LOCATION_PERMISSION_REQUEST = 1001

    // ViewModel
    private val viewModel: PlacesViewModel by viewModels()
    
    // Current result data for favorites
    private var currentRestaurantName: String? = null
    private var currentLocation: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initializeUI()
        setupLocationServices()
        setupNetworkMonitoring()
        setupClickListeners()
        observeViewModel()
    }

    /**
     * Initialize all UI components
     */
    private fun initializeUI() {
        locationInputLayout = findViewById(R.id.locationInputLayout)
        locationInput = findViewById(R.id.locationInput)
        searchButton = findViewById(R.id.searchButton)
        locateMeButton = findViewById(R.id.locateMeButton)
        outputText = findViewById(R.id.outputText)
        progressBar = findViewById(R.id.progressBar)
        loadingCard = findViewById(R.id.loadingCard)
        resultCard = findViewById(R.id.resultCard)
        loadingText = findViewById(R.id.loadingText)
        resultIcon = findViewById(R.id.resultIcon)
        favoriteIcon = findViewById(R.id.favoriteIcon)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        fabHistory = findViewById(R.id.fabHistory)
        
        // Network status components (optional - may not exist in current layout)
        try {
            networkStatusIndicator = findViewById(R.id.networkStatusIndicator)
            networkStatusIcon = findViewById(R.id.networkStatusIcon)
        } catch (e: Exception) {
            // Network status indicators not found in layout - initialize with dummy views
            // This is okay for now as we handle this gracefully in updateNetworkStatusIndicator
        }
        
        // Initially hide loading and result cards
        loadingCard.visibility = View.GONE
        resultCard.visibility = View.GONE
    }
    
    /**
     * Setup location services
     */
    private fun setupLocationServices() {
        locationClient = LocationServices.getFusedLocationProviderClient(this)
    }
    
    /**
     * Setup network monitoring for enhanced error handling
     */
    private fun setupNetworkMonitoring() {
        networkStatusManager = NetworkStatusManager(this)
        viewModel.initializeNetworkManager(this)
        
        // Observe network status changes
        lifecycleScope.launch {
            networkStatusManager?.networkStatus?.collect { networkStatus ->
                updateNetworkStatusIndicator(networkStatus)
            }
        }
    }
    
    /**
     * Update network status indicator UI
     */
    private fun updateNetworkStatusIndicator(networkStatus: NetworkStatus) {
        try {
            networkStatusIndicator?.let { indicator ->
                networkStatusIcon?.let { icon ->
                    val statusText = networkStatusManager?.getNetworkStatusDescription(this) ?: "Unknown"
                    indicator.text = statusText
                    
                    // Update network status icon and color
                    if (networkStatus.isConnected) {
                        icon.setImageResource(R.drawable.ic_wifi)
                        indicator.setTextColor(getColor(R.color.success_green))
                    } else {
                        icon.setImageResource(R.drawable.ic_wifi_off)
                        indicator.setTextColor(getColor(R.color.error_red))
                    }
                }
            }
        } catch (e: Exception) {
            // Handle gracefully if network status views are not available
        }
    }
    
    /**
     * Clean up network monitoring
     */
    override fun onDestroy() {
        super.onDestroy()
        networkStatusManager?.unregisterNetworkCallback()
    }
    
    /**
     * Setup click listeners for buttons
     */
    private fun setupClickListeners() {
        locateMeButton.setOnClickListener {
            getCurrentLocation()
        }
        
        searchButton.setOnClickListener {
            val locationText = locationInput.text.toString()
            hideKeyboard(it)
            
            // Clear any previous errors
            locationInputLayout.error = null
            
            // Always force refresh for main search button to get variety
            viewModel.searchFoodPlace(locationText, forceRefresh = true)
        }
        
        favoriteIcon.setOnClickListener {
            currentRestaurantName?.let { restaurantName ->
                currentLocation?.let { location ->
                    lifecycleScope.launch {
                        val wasFavorited = viewModel.isFavorite(restaurantName, location)
                        viewModel.toggleFavorite(restaurantName, location)
                        
                        if (wasFavorited) {
                            // Was favorited, now removed
                            favoriteIcon.setImageResource(R.drawable.ic_favorite_border)
                            Toast.makeText(this@MainActivity, "Removed from favorites! 💔", Toast.LENGTH_SHORT).show()
                        } else {
                            // Was not favorited, now added
                            favoriteIcon.setImageResource(R.drawable.ic_favorite_filled)
                            Toast.makeText(this@MainActivity, "Added to favorites! 💝", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
        
        swipeRefreshLayout.setOnRefreshListener {
            val locationText = locationInput.text.toString()
            if (locationText.isNotBlank()) {
                viewModel.searchFoodPlace(locationText, forceRefresh = true)
            } else {
                swipeRefreshLayout.isRefreshing = false
            }
        }
        
        fabHistory.setOnClickListener {
            val intent = Intent(this, com.gal1leo.hungrygf.ui.HistoryFavoritesActivity::class.java)
            startActivity(intent)
        }
    }
    
    /**
     * Observe ViewModel state changes
     */
    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                handleUiState(state)
            }
        }
    }

    /**
     * Hide soft keyboard
     */
    private fun hideKeyboard(view: View) {
        try {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Get user's current location
     */
    private fun getCurrentLocation() {
        // Check if the location permission is granted
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // If permission is not granted, request it from the user
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST
            )
            return
        }

        // Show loading state
        locationInput.setText("Getting location...")
        locateMeButton.isEnabled = false
        
        // Fetch the last known location with proper null safety
        locationClient.lastLocation
            .addOnSuccessListener { location ->
                locateMeButton.isEnabled = true
                if (location != null) {
                    // Validate location coordinates
                    val lat = location.latitude
                    val lon = location.longitude
                    
                    if (lat.isFinite() && lon.isFinite() && 
                        lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180) {
                        
                        lifecycleScope.launch {
                            try {
                                val cityName = getCityName(lat, lon)
                                // Only set if we got a valid city name
                                if (cityName.isNotBlank() && cityName != "Unknown location") {
                                    locationInput.setText(cityName)
                                } else {
                                    locationInput.setText("$lat, $lon")
                                }
                            } catch (e: Exception) {
                                println("Error getting city name: ${e.message}")
                                locationInput.setText("$lat, $lon")
                            }
                        }
                    } else {
                        locationInput.setText("Invalid location coordinates")
                    }
                } else {
                    locationInput.setText("Unable to get location")
                }
            }
            .addOnFailureListener { exception ->
                locateMeButton.isEnabled = true
                println("Location retrieval failed: ${exception.message}")
                locationInput.setText("Location unavailable")
            }
    }

    /**
     * Get city name from coordinates using geocoding
     */
    private suspend fun getCityName(latitude: Double, longitude: Double): String {
        return suspendCoroutine { continuation ->
            val executor = Executors.newSingleThreadExecutor()
            executor.execute {
                try {
                    // Validate coordinates
                    if (!latitude.isFinite() || !longitude.isFinite() ||
                        latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
                        continuation.resume("Invalid coordinates")
                        return@execute
                    }
                    
                    val geocoder = Geocoder(this, Locale.getDefault())
                    
                    // Safe geocoding with null checks
                    val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                    
                    val cityName = addresses?.let { addressList ->
                        if (addressList.isNotEmpty()) {
                            val address = addressList[0]
                            // Try different address components in order of preference
                            address.locality?.takeIf { it.isNotBlank() }
                                ?: address.subAdminArea?.takeIf { it.isNotBlank() }
                                ?: address.adminArea?.takeIf { it.isNotBlank() }
                                ?: address.countryName?.takeIf { it.isNotBlank() }
                        } else null
                    }

                    continuation.resume(cityName ?: "Unknown location")
                } catch (e: Exception) {
                    println("Geocoding error: ${e.message}")
                    continuation.resume("Unknown location")
                } finally {
                    executor.shutdown()
                }
            }
        }
    }

    /**
     * Shows validation error message to user with improved UI feedback
     */
    private fun showValidationError(message: String) {
        locationInputLayout.error = message
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        
        // Add a subtle shake animation to the input field
        locationInput.animate()
            .translationX(-10f)
            .setDuration(100)
            .withEndAction {
                locationInput.animate()
                    .translationX(10f)
                    .setDuration(100)
                    .withEndAction {
                        locationInput.animate()
                            .translationX(0f)
                            .setDuration(100)
                    }
            }
    }

    /**
     * Handle the result of the permission request
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // Check if the permission was granted
        if (requestCode == LOCATION_PERMISSION_REQUEST &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            // If permission is granted, fetch the location
            getCurrentLocation()
        } else {
            // If permission is denied, update the input with an error message
            locationInput.setText("Location access denied")
            Toast.makeText(this, "Location permission is required to use this feature", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Handle different UI states from the ViewModel with new modern UI
     */
    private fun handleUiState(state: UiState) {
        // Stop refresh indicator when state changes
        swipeRefreshLayout.isRefreshing = false
        
        when (state) {
            is UiState.Idle -> {
                loadingCard.visibility = View.GONE
                resultCard.visibility = View.GONE
                locationInputLayout.error = null
                outputText.text = ""
            }
            
            is UiState.Loading -> {
                loadingCard.visibility = View.VISIBLE
                resultCard.visibility = View.GONE
                loadingText.text = state.message
                searchButton.isEnabled = false
            }
            
            is UiState.Success -> {
                loadingCard.visibility = View.GONE
                resultCard.visibility = View.VISIBLE
                searchButton.isEnabled = true
                
                // Store current result data for favorites
                currentRestaurantName = state.restaurantName
                currentLocation = state.location
                
                outputText.text = "🍽️ Selected place: ${state.restaurantName}"
                outputText.setTextColor(getColor(R.color.success_green))
                resultIcon.setImageResource(R.drawable.ic_restaurant)
                favoriteIcon.visibility = View.VISIBLE
                
                // Check if restaurant is already favorited and set appropriate icon
                lifecycleScope.launch {
                    val isFavorited = viewModel.isFavorite(state.restaurantName, state.location)
                    favoriteIcon.setImageResource(
                        if (isFavorited) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_border
                    )
                }
                
                println("Search successful for ${state.location}: ${state.restaurantName}")
            }
            
            is UiState.Empty -> {
                loadingCard.visibility = View.GONE
                resultCard.visibility = View.VISIBLE
                searchButton.isEnabled = true
                
                // Clear current result data
                currentRestaurantName = null
                currentLocation = null
                favoriteIcon.visibility = View.GONE
                
                outputText.text = "😔 No restaurants found for '${state.searchLocation}'"
                outputText.setTextColor(getColor(R.color.warning_amber))
                resultIcon.setImageResource(R.drawable.ic_search)
                
                if (state.suggestions.isNotEmpty()) {
                    val suggestionText = state.suggestions.joinToString("\n• ", prefix = "\n\n💡 Try:\n• ")
                    outputText.append(suggestionText)
                }
            }
            
            is UiState.ValidationError -> {
                loadingCard.visibility = View.GONE
                resultCard.visibility = View.GONE
                searchButton.isEnabled = true
                showValidationError(state.message)
            }
            
            is UiState.Error -> {
                loadingCard.visibility = View.GONE
                resultCard.visibility = View.VISIBLE
                searchButton.isEnabled = true
                
                // Clear current result data
                currentRestaurantName = null
                currentLocation = null
                favoriteIcon.visibility = View.GONE
                
                // Enhanced error display with specific icons and colors
                val errorIcon = getErrorIcon(state.errorType)
                val errorColor = getErrorColor(state.errorType)
                
                outputText.text = "$errorIcon ${state.message}"
                outputText.setTextColor(getColor(errorColor))
                resultIcon.setImageResource(getErrorResultIcon(state.errorType))
                
                // Show suggestions if available
                if (state.suggestions.isNotEmpty()) {
                    val suggestionText = state.suggestions.joinToString("\n• ", prefix = "\n\n💡 Suggestions:\n• ")
                    outputText.append(suggestionText)
                }
                
                // Show network status if needed
                if (state.needsNetworkCheck) {
                    val networkStatus = networkStatusManager?.getNetworkStatusDescription(this)
                    if (!networkStatus.isNullOrBlank()) {
                        outputText.append("\n\n📡 Network: $networkStatus")
                    }
                }
                
                // Add action buttons
                if (state.actionButtons.isNotEmpty()) {
                    showErrorActionButtons(state.actionButtons, state.isRetryable)
                } else if (state.isRetryable) {
                    // Fallback to simple retry
                    val retryButton = "\n\n[Tap to retry search]"
                    outputText.append(retryButton)
                    
                    outputText.setOnClickListener {
                        val locationText = locationInput.text.toString()
                        if (locationText.isNotBlank()) {
                            Toast.makeText(this, getString(R.string.toast_retrying_request), Toast.LENGTH_SHORT).show()
                            viewModel.searchFoodPlace(locationText)
                        }
                    }
                } else {
                    outputText.append("\n\n🔧 Please contact support if this persists")
                    outputText.setOnClickListener(null)
                }
            }
        }
    }
    
    /**
     * Get appropriate error icon based on error type
     */
    private fun getErrorIcon(errorType: ErrorType): String {
        return when (errorType) {
            ErrorType.NETWORK_CONNECTION -> "📡"
            ErrorType.NETWORK_TIMEOUT -> "⏱️"
            ErrorType.NETWORK_SERVER -> "🌐"
            ErrorType.API_AUTHENTICATION -> "🔐"
            ErrorType.API_RATE_LIMIT -> "🚦"
            ErrorType.API_QUOTA_EXCEEDED -> "📊"
            ErrorType.LOCATION_PERMISSION -> "📍"
            ErrorType.LOCATION_UNAVAILABLE -> "🗺️"
            ErrorType.VALIDATION -> "✏️"
            ErrorType.GENERIC -> "🚫"
        }
    }
    
    /**
     * Get appropriate error color based on error type
     */
    private fun getErrorColor(errorType: ErrorType): Int {
        return when (errorType) {
            ErrorType.NETWORK_CONNECTION -> R.color.error_red
            ErrorType.NETWORK_TIMEOUT -> R.color.warning_amber
            ErrorType.NETWORK_SERVER -> R.color.error_red
            ErrorType.API_AUTHENTICATION -> R.color.error_red
            ErrorType.API_RATE_LIMIT -> R.color.warning_amber
            ErrorType.API_QUOTA_EXCEEDED -> R.color.warning_amber
            ErrorType.LOCATION_PERMISSION -> R.color.warning_amber
            ErrorType.LOCATION_UNAVAILABLE -> R.color.warning_amber
            ErrorType.VALIDATION -> R.color.warning_amber
            ErrorType.GENERIC -> R.color.error_red
        }
    }
    
    /**
     * Get appropriate result icon based on error type
     */
    private fun getErrorResultIcon(errorType: ErrorType): Int {
        return when (errorType) {
            ErrorType.NETWORK_CONNECTION -> R.drawable.ic_wifi_off
            ErrorType.NETWORK_TIMEOUT, ErrorType.NETWORK_SERVER -> R.drawable.ic_cloud_off
            ErrorType.API_AUTHENTICATION, ErrorType.API_RATE_LIMIT, ErrorType.API_QUOTA_EXCEEDED -> R.drawable.ic_key
            ErrorType.LOCATION_PERMISSION, ErrorType.LOCATION_UNAVAILABLE -> R.drawable.ic_location_off
            ErrorType.VALIDATION -> R.drawable.ic_edit
            ErrorType.GENERIC -> R.drawable.ic_error
        }
    }
    
    /**
     * Show enhanced error action buttons
     */
    private fun showErrorActionButtons(actionButtons: List<ErrorAction>, isRetryable: Boolean) {
        val buttonTexts = mutableListOf<String>()
        
        actionButtons.forEach { action ->
            if (action.isEnabled) {
                buttonTexts.add("[${action.label}]")
            }
        }
        
        if (buttonTexts.isNotEmpty()) {
            val buttonsText = "\n\n" + buttonTexts.joinToString("  ")
            outputText.append(buttonsText)
        }
        
        // Set click listener to handle action buttons
        outputText.setOnClickListener {
            handleErrorActionClick(actionButtons, isRetryable)
        }
    }
    
    /**
     * Handle clicks on error action buttons
     */
    private fun handleErrorActionClick(actionButtons: List<ErrorAction>, isRetryable: Boolean) {
        if (actionButtons.isNotEmpty()) {
            val action = actionButtons.first() // For simplicity, handle the first action
            when (action.actionType) {
                ActionType.RETRY, ActionType.RETRY_WITH_REFRESH -> {
                    val locationText = locationInput.text.toString()
                    if (locationText.isNotBlank()) {
                        Toast.makeText(this, getString(R.string.toast_retrying_request), Toast.LENGTH_SHORT).show()
                        val forceRefresh = action.actionType == ActionType.RETRY_WITH_REFRESH
                        viewModel.searchFoodPlace(locationText, forceRefresh)
                    }
                }
                ActionType.CHECK_CONNECTION -> {
                    val networkStatus = networkStatusManager?.getNetworkStatusDescription(this)
                    Toast.makeText(this, "Network: $networkStatus", Toast.LENGTH_LONG).show()
                }
                ActionType.OPEN_SETTINGS -> {
                    try {
                        val intent = android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(this, "Unable to open settings", Toast.LENGTH_SHORT).show()
                    }
                }
                ActionType.CONTACT_SUPPORT -> {
                    Toast.makeText(this, "Please contact support at support@hungrygf.com", Toast.LENGTH_LONG).show()
                }
                ActionType.TRY_DIFFERENT_LOCATION -> {
                    locationInput.requestFocus()
                    locationInput.selectAll()
                    Toast.makeText(this, "Try entering a different location", Toast.LENGTH_SHORT).show()
                }
            }
        } else if (isRetryable) {
            // Fallback retry
            val locationText = locationInput.text.toString()
            if (locationText.isNotBlank()) {
                Toast.makeText(this, getString(R.string.toast_retrying_request), Toast.LENGTH_SHORT).show()
                viewModel.searchFoodPlace(locationText)
            }
        }
    }

}
