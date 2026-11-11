package com.dealfinder.di

import android.content.Context
import androidx.room.Room
import com.dealfinder.data.dao.BookingDao
import com.dealfinder.data.dao.CategoryDao
import com.dealfinder.data.dao.FavoriteDao
import com.dealfinder.data.dao.OfferDao
import com.dealfinder.data.dao.ProviderDao
import com.dealfinder.data.dao.ServiceDao
import com.dealfinder.data.dao.UserDao
import com.dealfinder.data.dao.VerifiedRatingDao
import com.dealfinder.data.database.AppDatabase
import com.dealfinder.data.database.DemoDataInitializer
import com.dealfinder.data.repository.BookingRepository
import com.dealfinder.data.repository.CategoryRepository
import com.dealfinder.data.repository.FavoriteRepository
import com.dealfinder.data.repository.OfferRepository
import com.dealfinder.data.repository.ProviderRepository
import com.dealfinder.data.repository.RatingRepository
import com.dealfinder.data.repository.ServiceRepository
import com.dealfinder.data.repository.UserRepository
import com.dealfinder.data.repository.impl.RoomBookingRepository
import com.dealfinder.data.repository.impl.RoomCategoryRepository
import com.dealfinder.data.repository.impl.RoomFavoriteRepository
import com.dealfinder.data.repository.impl.RoomOfferRepository
import com.dealfinder.data.repository.impl.RoomProviderRepository
import com.dealfinder.data.repository.impl.RoomRatingRepository
import com.dealfinder.data.repository.impl.RoomServiceRepository
import com.dealfinder.data.repository.impl.RoomUserRepository
import com.dealfinder.util.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.withLock

/**
 * Simple dependency injection module
 * Creates and provides all dependencies for the app
 */
object AppModule {
    
    private var database: AppDatabase? = null
    private var demoDataInitializer: DemoDataInitializer? = null
    private var appPreferences: AppPreferences? = null
    
    // Repositories
    private var userRepository: UserRepository? = null
    private var providerRepository: ProviderRepository? = null
    private var serviceRepository: ServiceRepository? = null
    private var ratingRepository: RatingRepository? = null
    private var offerRepository: OfferRepository? = null
    private var bookingRepository: BookingRepository? = null
    private var favoriteRepository: FavoriteRepository? = null
    private var categoryRepository: CategoryRepository? = null
    
    /**
     * Initialize the module with application context.
     * Idempotent – mehrfache Aufrufe (Application + Activity + Tests) erzeugen
     * keine zusätzlichen DB-Instanzen.
     */
    fun initialize(context: Context) {
        if (database != null) return

        // Create database.
        // Version 2 added the saved_offers table; destructive fallback re-seeds the
        // demo data afterwards, which is acceptable for this prototype.
        database = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        ).fallbackToDestructiveMigration().build()
        
        // Create DAOs
        val userDao = database!!.userDao()
        val categoryDao = database!!.categoryDao()
        val providerDao = database!!.providerDao()
        val serviceDao = database!!.serviceDao()
        val ratingDao = database!!.verifiedRatingDao()
        val offerDao = database!!.offerDao()
        val bookingDao = database!!.bookingDao()
        val favoriteDao = database!!.favoriteDao()
        
        // Initialize demo data
        demoDataInitializer = DemoDataInitializer(
            userDao, categoryDao, providerDao, serviceDao, ratingDao, offerDao, bookingDao, favoriteDao
        )
        
        // Create preferences
        appPreferences = AppPreferences(context)
        
        // Initialize repositories
        userRepository = RoomUserRepository(userDao)
        providerRepository = RoomProviderRepository(providerDao)
        serviceRepository = RoomServiceRepository(serviceDao)
        ratingRepository = RoomRatingRepository(ratingDao)
        offerRepository = RoomOfferRepository(offerDao, database!!.savedOfferDao())
        bookingRepository = RoomBookingRepository(bookingDao)
        favoriteRepository = RoomFavoriteRepository(favoriteDao)
        categoryRepository = RoomCategoryRepository(categoryDao)
    }
    
    /**
     * Initialize demo data (call this once at app startup)
     */
    suspend fun initializeDemoData() {
        demoDataInitializer?.initialize()
    }
    
    // Repository providers
    fun provideUserRepository(): UserRepository {
        return userRepository ?: throw IllegalStateException("AppModule not initialized")
    }
    
    fun provideProviderRepository(): ProviderRepository {
        return providerRepository ?: throw IllegalStateException("AppModule not initialized")
    }
    
    fun provideServiceRepository(): ServiceRepository {
        return serviceRepository ?: throw IllegalStateException("AppModule not initialized")
    }
    
    fun provideRatingRepository(): RatingRepository {
        return ratingRepository ?: throw IllegalStateException("AppModule not initialized")
    }
    
    fun provideOfferRepository(): OfferRepository {
        return offerRepository ?: throw IllegalStateException("AppModule not initialized")
    }
    
    fun provideBookingRepository(): BookingRepository {
        return bookingRepository ?: throw IllegalStateException("AppModule not initialized")
    }
    
    fun provideFavoriteRepository(): FavoriteRepository {
        return favoriteRepository ?: throw IllegalStateException("AppModule not initialized")
    }
    
    fun provideCategoryRepository(): CategoryRepository {
        return categoryRepository ?: throw IllegalStateException("AppModule not initialized")
    }
    
    fun provideAppPreferences(): AppPreferences {
        return appPreferences ?: throw IllegalStateException("AppModule not initialized")
    }
    
    /**
     * Clear all data (for demo reset).
     * Läuft unter demselben Sperr-Mutex wie die Demo-Initialisierung, damit
     * Reset und Seeding sich nicht überlappen.
     */
    suspend fun clearAllData() {
        com.dealfinder.data.database.DemoDataInitializer.lock.withLock {
            database?.clearAllTables()
        }
    }
}
