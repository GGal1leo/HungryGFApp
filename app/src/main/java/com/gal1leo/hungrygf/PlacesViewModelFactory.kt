package com.gal1leo.hungrygf

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.gal1leo.hungrygf.repository.EnhancedPlacesRepository

/**
 * Factory class for creating PlacesViewModel instances with dependency injection
 */
class PlacesViewModelFactory(
    private val repository: EnhancedPlacesRepository
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlacesViewModel::class.java)) {
            return PlacesViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
