package com.gal1leo.hungrygf

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertFailsWith

/**
 * Unit tests for PlacesRepository using MockWebServer
 */
class PlacesRepositoryTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var repository: PlacesRepository
    private lateinit var apiService: PlacesApiService

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        apiService = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PlacesApiService::class.java)

        repository = PlacesRepository(apiService)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `findFoodPlaces with empty location throws IllegalArgumentException`() = runTest {
        assertFailsWith<IllegalArgumentException> {
            repository.findFoodPlaces("", "test-api-key")
        }
    }

    @Test
    fun `findFoodPlaces with blank location throws IllegalArgumentException`() = runTest {
        assertFailsWith<IllegalArgumentException> {
            repository.findFoodPlaces("   ", "test-api-key")
        }
    }

    @Test
    fun `findFoodPlaces with empty API key throws IllegalArgumentException`() = runTest {
        assertFailsWith<IllegalArgumentException> {
            repository.findFoodPlaces("New York", "")
        }
    }

    @Test
    fun `findFoodPlaces with valid response returns restaurant name`() = runTest {
        val mockResponse = """
            {
                "places": [
                    {
                        "displayName": {
                            "text": "Test Restaurant"
                        },
                        "formattedAddress": "123 Test St",
                        "priceLevel": "PRICE_LEVEL_MODERATE",
                        "rating": 4.5,
                        "currentOpeningHours": {
                            "nextCloseTime": "2024-05-25T22:00:00Z"
                        }
                    }
                ]
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setBody(mockResponse)
                .setResponseCode(200)
        )

        val result = repository.findFoodPlaces("New York", "test-api-key")
        assertNotNull(result)
        assertEquals("Test Restaurant", result)
    }

    @Test
    fun `findFoodPlaces with empty places list returns null`() = runTest {
        val mockResponse = """
            {
                "places": []
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setBody(mockResponse)
                .setResponseCode(200)
        )

        val result = repository.findFoodPlaces("Unknown Location", "test-api-key")
        assertNull(result)
    }

    @Test
    fun `findFoodPlaces with places that have no opening hours returns null`() = runTest {
        val mockResponse = """
            {
                "places": [
                    {
                        "displayName": {
                            "text": "Test Restaurant"
                        },
                        "formattedAddress": "123 Test St",
                        "priceLevel": "PRICE_LEVEL_MODERATE",
                        "rating": 4.5,
                        "currentOpeningHours": null
                    }
                ]
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setBody(mockResponse)
                .setResponseCode(200)
        )

        val result = repository.findFoodPlaces("New York", "test-api-key")
        assertNull(result)
    }

    @Test
    fun `findFoodPlaces with places that have blank name are filtered out`() = runTest {
        val mockResponse = """
            {
                "places": [
                    {
                        "displayName": {
                            "text": ""
                        },
                        "formattedAddress": "123 Test St",
                        "priceLevel": "PRICE_LEVEL_MODERATE",
                        "rating": 4.5,
                        "currentOpeningHours": {
                            "nextCloseTime": "2024-05-25T22:00:00Z"
                        }
                    },
                    {
                        "displayName": {
                            "text": "Valid Restaurant"
                        },
                        "formattedAddress": "456 Test Ave",
                        "priceLevel": "PRICE_LEVEL_MODERATE",
                        "rating": 4.0,
                        "currentOpeningHours": {
                            "nextCloseTime": "2024-05-25T22:00:00Z"
                        }
                    }
                ]
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setBody(mockResponse)
                .setResponseCode(200)
        )

        val result = repository.findFoodPlaces("New York", "test-api-key")
        assertNotNull(result)
        assertEquals("Valid Restaurant", result)
    }

    @Test
    fun `findFoodPlaces with network error throws exception`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(500)
        )

        assertFailsWith<Exception> {
            repository.findFoodPlaces("New York", "test-api-key")
        }
    }
}
