package com.gal1leo.hungrygf

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.telephony.TelephonyManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages network connectivity status and provides real-time updates
 */
class NetworkStatusManager(private val context: Context) {
    
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    private val _networkStatus = MutableStateFlow(
        NetworkStatus(
            isConnected = isNetworkAvailable(),
            connectionType = getCurrentConnectionType(),
            signalStrength = getCurrentSignalStrength()
        )
    )
    val networkStatus: StateFlow<NetworkStatus> = _networkStatus.asStateFlow()
    
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            updateNetworkStatus()
        }
        
        override fun onLost(network: Network) {
            updateNetworkStatus()
        }
        
        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            updateNetworkStatus()
        }
    }
    
    init {
        registerNetworkCallback()
    }
    
    private fun registerNetworkCallback() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        try {
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        } catch (e: Exception) {
            // Handle registration failure gracefully
        }
    }
    
    fun unregisterNetworkCallback() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            // Handle unregistration failure gracefully
        }
    }
    
    private fun updateNetworkStatus() {
        _networkStatus.value = NetworkStatus(
            isConnected = isNetworkAvailable(),
            connectionType = getCurrentConnectionType(),
            signalStrength = getCurrentSignalStrength()
        )
    }
    
    private fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
    
    private fun getCurrentConnectionType(): ConnectionType {
        val network = connectivityManager.activeNetwork ?: return ConnectionType.UNKNOWN
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return ConnectionType.UNKNOWN
        
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> ConnectionType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> ConnectionType.MOBILE
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> ConnectionType.ETHERNET
            else -> ConnectionType.UNKNOWN
        }
    }
    
    private fun getCurrentSignalStrength(): SignalStrength {
        val network = connectivityManager.activeNetwork ?: return SignalStrength.UNKNOWN
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return SignalStrength.UNKNOWN
        
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                // For WiFi, we can't easily get signal strength, so return good by default if connected
                if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                    SignalStrength.GOOD
                } else {
                    SignalStrength.POOR
                }
            }
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                try {
                    val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                    // This would require more complex implementation for actual signal strength
                    // For now, return good if connected
                    SignalStrength.GOOD
                } catch (e: Exception) {
                    SignalStrength.UNKNOWN
                }
            }
            else -> SignalStrength.UNKNOWN
        }
    }
    
    /**
     * Get a user-friendly description of the current network status
     */
    fun getNetworkStatusDescription(context: Context): String {
        val status = _networkStatus.value
        return when {
            !status.isConnected -> context.getString(R.string.network_status_disconnected)
            else -> {
                val connectionTypeStr = when (status.connectionType) {
                    ConnectionType.WIFI -> context.getString(R.string.network_wifi)
                    ConnectionType.MOBILE -> context.getString(R.string.network_mobile)
                    ConnectionType.ETHERNET -> context.getString(R.string.network_ethernet)
                    ConnectionType.UNKNOWN -> context.getString(R.string.network_unknown)
                }
                "${context.getString(R.string.network_status_connected)} ($connectionTypeStr)"
            }
        }
    }
    
    /**
     * Get network-specific error suggestions
     */
    fun getNetworkErrorSuggestions(context: Context): List<String> {
        val status = _networkStatus.value
        val suggestions = mutableListOf<String>()
        
        if (!status.isConnected) {
            suggestions.add(context.getString(R.string.suggestion_check_internet))
            when (status.connectionType) {
                ConnectionType.MOBILE -> {
                    suggestions.add(context.getString(R.string.suggestion_move_to_better_signal))
                }
                ConnectionType.WIFI -> {
                    suggestions.add("Check your WiFi connection")
                }
                else -> {
                    suggestions.add("Check your network settings")
                }
            }
        } else if (status.signalStrength == SignalStrength.POOR) {
            suggestions.add(context.getString(R.string.suggestion_move_to_better_signal))
        }
        
        suggestions.add(context.getString(R.string.suggestion_try_again_later))
        return suggestions
    }
}
