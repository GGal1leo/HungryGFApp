package com.gal1leo.hungrygf

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for MainActivity UI
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class MainActivityUITest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun testUIElementsAreDisplayed() {
        // Check if all main UI elements are displayed
        onView(withId(R.id.locationInput))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.button))
            .check(matches(isDisplayed()))
            .check(matches(withText("Where is you hungry?")))
        
        onView(withId(R.id.locateMeButton))
            .check(matches(isDisplayed()))
            .check(matches(withText("Get Current Location")))
        
        onView(withId(R.id.outputText))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.progressBar))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testEmptyLocationShowsValidationError() {
        // Clear any existing text and leave empty
        onView(withId(R.id.locationInput))
            .perform(clearText())
        
        // Click search button
        onView(withId(R.id.button))
            .perform(click())
        
        // Check that validation error is shown
        onView(withId(R.id.outputText))
            .check(matches(withText(containsString("Please enter a location"))))
    }

    @Test
    fun testShortLocationShowsValidationError() {
        // Enter a single character
        onView(withId(R.id.locationInput))
            .perform(clearText(), typeText("a"))
        
        // Click search button
        onView(withId(R.id.button))
            .perform(click())
        
        // Check that validation error is shown
        onView(withId(R.id.outputText))
            .check(matches(withText(containsString("Location must be at least 2 characters"))))
    }

    @Test
    fun testInvalidCharactersShowValidationError() {
        // Enter location with invalid characters
        onView(withId(R.id.locationInput))
            .perform(clearText(), typeText("New@York"))
        
        // Click search button
        onView(withId(R.id.button))
            .perform(click())
        
        // Check that validation error is shown
        onView(withId(R.id.outputText))
            .check(matches(withText(containsString("Please enter a valid location"))))
    }

    @Test
    fun testValidLocationStartsSearch() {
        // Enter a valid location
        onView(withId(R.id.locationInput))
            .perform(clearText(), typeText("New York"))
        
        // Click search button
        onView(withId(R.id.button))
            .perform(click())
        
        // Check that search starts (loading message appears)
        // Note: This test might be flaky depending on network speed
        // In a real scenario, we'd mock the network calls
        onView(withId(R.id.outputText))
            .check(matches(withText(containsString("Searching"))))
    }

    @Test
    fun testLocationInputAcceptsValidInput() {
        val validLocations = listOf(
            "New York",
            "London",
            "123 Main Street",
            "San Francisco, CA"
        )
        
        validLocations.forEach { location ->
            onView(withId(R.id.locationInput))
                .perform(clearText(), typeText(location))
                .check(matches(withText(location)))
        }
    }
}
