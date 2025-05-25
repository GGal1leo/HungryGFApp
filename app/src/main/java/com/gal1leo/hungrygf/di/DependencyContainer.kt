package com.gal1leo.hungrygf.di

import android.content.Context
import androidx.room.Room
import com.gal1leo.hungrygf.data.HungryGFDatabase
import com.gal1leo.hungrygf.repository.EnhancedPlacesRepository
import com.gal1leo.hungrygf.PlacesApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Simple dependency injection container
 * This replaces Hilt temporarily to avoid compatibility issues
 */
object DependencyContainer {
    private lateinit var database: HungryGFDatabase
    private lateinit var apiService: PlacesApiService
    private lateinit var repository: EnhancedPlacesRepository
    
    fun initialize(context: Context) {
        // Initialize database
        database = Room.databaseBuilder(
            context.applicationContext,
            HungryGFDatabase::class.java,
            "hungry_gf_database"
        ).build()
        
        // Initialize network
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
        
        val retrofit = Retrofit.Builder()
            .baseUrl("https://places.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        apiService = retrofit.create(PlacesApiService::class.java)
        
        // Initialize repository
        repository = EnhancedPlacesRepository(
            apiService = apiService,
            searchHistoryDao = database.searchHistoryDao(),
            favoriteDao = database.favoriteRestaurantDao(),
            cachedResultDao = database.cachedResultDao()
        )
    }
    
    fun getRepository(): EnhancedPlacesRepository = repository
    val placesRepository: EnhancedPlacesRepository get() = repository
    fun getDatabase(): HungryGFDatabase = database
}
