package com.dealfinder

import android.app.Application
import com.dealfinder.di.AppModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Custom Application class for Dealfinder
 */
class DealfinderApp : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize DI module
        AppModule.initialize(this)
        
        // Initialize demo data in background
        CoroutineScope(Dispatchers.IO).launch {
            AppModule.initializeDemoData()
        }
    }
}
