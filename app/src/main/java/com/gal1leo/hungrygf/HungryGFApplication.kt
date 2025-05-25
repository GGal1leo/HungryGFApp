package com.gal1leo.hungrygf

import android.app.Application
import com.gal1leo.hungrygf.di.DependencyContainer

/**
 * Application class for Hungry GF app
 * Initializes dependency injection container
 */
class HungryGFApplication : Application() {
    
    val dependencyContainer: DependencyContainer
        get() = DependencyContainer
    
    override fun onCreate() {
        super.onCreate()
        DependencyContainer.initialize(this)
    }
}
