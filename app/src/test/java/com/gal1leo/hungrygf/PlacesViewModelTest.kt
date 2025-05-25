package com.gal1leo.hungrygf

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for PlacesViewModel
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PlacesViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var mockRepository: PlacesRepository

    private lateinit var viewModel: PlacesViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        
        // Note: In a real implementation, we'd inject the repository through DI
        // For now, we'll test the validation logic which doesn't require repository mocking
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should be Idle`() = runTest {
        viewModel = PlacesViewModel()
        
        viewModel.uiState.test {
            assertEquals(UiState.Idle, awaitItem())
        }
    }

    @Test
    fun `searchFoodPlace with empty location should emit ValidationError`() = runTest {
        viewModel = PlacesViewModel()
        
        viewModel.uiState.test {
            // Skip initial Idle state
            assertEquals(UiState.Idle, awaitItem())
            
            viewModel.searchFoodPlace("")
            
            val state = awaitItem()
            assertTrue(state is UiState.ValidationError)
            assertEquals("Please enter a location", state.message)
        }
    }

    @Test
    fun `searchFoodPlace with too short location should emit ValidationError`() = runTest {
        viewModel = PlacesViewModel()
        
        viewModel.uiState.test {
            // Skip initial Idle state
            assertEquals(UiState.Idle, awaitItem())
            
            viewModel.searchFoodPlace("a")
            
            val state = awaitItem()
            assertTrue(state is UiState.ValidationError)
            assertEquals("Location must be at least 2 characters", state.message)
        }
    }

    @Test
    fun `searchFoodPlace with invalid characters should emit ValidationError`() = runTest {
        viewModel = PlacesViewModel()
        
        viewModel.uiState.test {
            // Skip initial Idle state
            assertEquals(UiState.Idle, awaitItem())
            
            viewModel.searchFoodPlace("New@York")
            
            val state = awaitItem()
            assertTrue(state is UiState.ValidationError)
            assertEquals("Please enter a valid location (letters, numbers, spaces, and common punctuation only)", state.message)
        }
    }

    @Test
    fun `searchFoodPlace with unreasonable location should emit ValidationError`() = runTest {
        viewModel = PlacesViewModel()
        
        viewModel.uiState.test {
            // Skip initial Idle state
            assertEquals(UiState.Idle, awaitItem())
            
            viewModel.searchFoodPlace("test@example.com")
            
            val state = awaitItem()
            assertTrue(state is UiState.ValidationError)
            assertEquals("This doesn't look like a valid location", state.message)
        }
    }

    @Test
    fun `resetState should emit Idle state`() = runTest {
        viewModel = PlacesViewModel()
        
        viewModel.uiState.test {
            // Skip initial Idle state
            assertEquals(UiState.Idle, awaitItem())
            
            // Search to change state
            viewModel.searchFoodPlace("")
            
            // Skip ValidationError state
            awaitItem()
            
            // Reset state
            viewModel.resetState()
            
            val state = awaitItem()
            assertEquals(UiState.Idle, state)
        }
    }
}
