package com.gal1leo.hungrygf

/**
 * Input validation utilities for the app
 */
object InputValidator {
    
    /**
     * Validates location input and returns validation result
     */
    fun validateLocation(location: String?): ValidationResult {
        return when {
            location.isNullOrBlank() -> ValidationResult.Error("Please enter a location")
            location.trim().length < 2 -> ValidationResult.Error("Location must be at least 2 characters")
            location.trim().length > 100 -> ValidationResult.Error("Location is too long (max 100 characters)")
            !isValidLocationFormat(location.trim()) -> ValidationResult.Error("Please enter a valid location (letters, numbers, spaces, and common punctuation only)")
            else -> ValidationResult.Success(location.trim())
        }
    }
    
    /**
     * Checks if location contains only valid characters
     */
    private fun isValidLocationFormat(location: String): Boolean {
        // Allow letters, numbers, spaces, commas, periods, hyphens, apostrophes, and parentheses
        val validLocationRegex = "^[a-zA-Z0-9\\s,.'()-]+$".toRegex()
        return location.matches(validLocationRegex)
    }
    
    /**
     * Validates if the input looks like a potential city/address
     */
    fun isReasonableLocation(location: String): Boolean {
        val trimmed = location.trim()
        
        // Check for common location patterns
        return when {
            // Too short to be meaningful
            trimmed.length < 2 -> false
            
            // Contains at least one letter (not just numbers/symbols)
            !trimmed.any { it.isLetter() } -> false
            
            // Common spam patterns
            trimmed.contains("@") || 
            trimmed.contains("http") || 
            trimmed.contains("www.") -> false
            
            // Excessive repetition (like "aaaaaaa")
            hasExcessiveRepetition(trimmed) -> false
            
            else -> true
        }
    }
    
    /**
     * Checks for excessive character repetition
     */
    private fun hasExcessiveRepetition(text: String): Boolean {
        if (text.length < 4) return false
        
        var repeatCount = 1
        var maxRepeat = 1
        
        for (i in 1 until text.length) {
            if (text[i] == text[i-1]) {
                repeatCount++
                maxRepeat = maxOf(maxRepeat, repeatCount)
            } else {
                repeatCount = 1
            }
        }
        
        // Allow up to 3 consecutive same characters (like "Mississippi")
        return maxRepeat > 3
    }
    
    /**
     * Suggests corrections for common input mistakes
     */
    fun getSuggestions(location: String): List<String> {
        val suggestions = mutableListOf<String>()
        
        when {
            location.isBlank() -> {
                suggestions.addAll(listOf(
                    "Try: New York",
                    "Try: London", 
                    "Try: Your city name"
                ))
            }
            location.length == 1 -> {
                suggestions.add("Enter a full city name or address")
            }
            !location.any { it.isLetter() } -> {
                suggestions.add("Include letters in your location (e.g., 'New York' not '123')")
            }
            location.contains("@") -> {
                suggestions.add("Enter a location, not an email address")
            }
        }
        
        return suggestions
    }
}

/**
 * Sealed class representing validation results
 */
sealed class ValidationResult {
    data class Success(val validInput: String) : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}
