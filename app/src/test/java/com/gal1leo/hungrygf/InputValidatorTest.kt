package com.gal1leo.hungrygf

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for InputValidator
 */
class InputValidatorTest {

    @Test
    fun `validateLocation returns error for null input`() {
        val result = InputValidator.validateLocation(null)
        assertTrue(result is ValidationResult.Error)
        assertEquals("Please enter a location", (result as ValidationResult.Error).message)
    }

    @Test
    fun `validateLocation returns error for empty input`() {
        val result = InputValidator.validateLocation("")
        assertTrue(result is ValidationResult.Error)
        assertEquals("Please enter a location", (result as ValidationResult.Error).message)
    }

    @Test
    fun `validateLocation returns error for blank input`() {
        val result = InputValidator.validateLocation("   ")
        assertTrue(result is ValidationResult.Error)
        assertEquals("Please enter a location", (result as ValidationResult.Error).message)
    }

    @Test
    fun `validateLocation returns error for too short input`() {
        val result = InputValidator.validateLocation("a")
        assertTrue(result is ValidationResult.Error)
        assertEquals("Location must be at least 2 characters", (result as ValidationResult.Error).message)
    }

    @Test
    fun `validateLocation returns error for too long input`() {
        val longLocation = "a".repeat(101)
        val result = InputValidator.validateLocation(longLocation)
        assertTrue(result is ValidationResult.Error)
        assertEquals("Location is too long (max 100 characters)", (result as ValidationResult.Error).message)
    }

    @Test
    fun `validateLocation returns error for invalid characters`() {
        val result = InputValidator.validateLocation("New@York#")
        assertTrue(result is ValidationResult.Error)
        assertEquals("Please enter a valid location (letters, numbers, spaces, and common punctuation only)", (result as ValidationResult.Error).message)
    }

    @Test
    fun `validateLocation returns success for valid city name`() {
        val result = InputValidator.validateLocation("New York")
        assertTrue(result is ValidationResult.Success)
        assertEquals("New York", (result as ValidationResult.Success).validInput)
    }

    @Test
    fun `validateLocation returns success for valid address`() {
        val result = InputValidator.validateLocation("123 Main St, Springfield")
        assertTrue(result is ValidationResult.Success)
        assertEquals("123 Main St, Springfield", (result as ValidationResult.Success).validInput)
    }

    @Test
    fun `validateLocation trims whitespace`() {
        val result = InputValidator.validateLocation("  London  ")
        assertTrue(result is ValidationResult.Success)
        assertEquals("London", (result as ValidationResult.Success).validInput)
    }

    @Test
    fun `isReasonableLocation returns false for too short location`() {
        assertFalse(InputValidator.isReasonableLocation("a"))
    }

    @Test
    fun `isReasonableLocation returns false for numbers only`() {
        assertFalse(InputValidator.isReasonableLocation("12345"))
    }

    @Test
    fun `isReasonableLocation returns false for email address`() {
        assertFalse(InputValidator.isReasonableLocation("test@example.com"))
    }

    @Test
    fun `isReasonableLocation returns false for URL`() {
        assertFalse(InputValidator.isReasonableLocation("http://example.com"))
        assertFalse(InputValidator.isReasonableLocation("www.example.com"))
    }

    @Test
    fun `isReasonableLocation returns false for excessive repetition`() {
        assertFalse(InputValidator.isReasonableLocation("aaaaaaa"))
    }

    @Test
    fun `isReasonableLocation returns true for valid city names`() {
        assertTrue(InputValidator.isReasonableLocation("New York"))
        assertTrue(InputValidator.isReasonableLocation("London"))
        assertTrue(InputValidator.isReasonableLocation("San Francisco"))
    }

    @Test
    fun `isReasonableLocation returns true for addresses`() {
        assertTrue(InputValidator.isReasonableLocation("123 Main Street"))
        assertTrue(InputValidator.isReasonableLocation("45 Oak Ave, Boston"))
    }

    @Test
    fun `getSuggestions returns default suggestions for blank input`() {
        val suggestions = InputValidator.getSuggestions("")
        assertTrue(suggestions.contains("Try: New York"))
        assertTrue(suggestions.contains("Try: London"))
        assertTrue(suggestions.contains("Try: Your city name"))
    }

    @Test
    fun `getSuggestions returns helpful message for single character`() {
        val suggestions = InputValidator.getSuggestions("a")
        assertTrue(suggestions.contains("Enter a full city name or address"))
    }

    @Test
    fun `getSuggestions returns helpful message for numbers only`() {
        val suggestions = InputValidator.getSuggestions("123")
        assertTrue(suggestions.contains("Include letters in your location (e.g., 'New York' not '123')"))
    }

    @Test
    fun `getSuggestions returns helpful message for email format`() {
        val suggestions = InputValidator.getSuggestions("test@example.com")
        assertTrue(suggestions.contains("Enter a location, not an email address"))
    }
}
