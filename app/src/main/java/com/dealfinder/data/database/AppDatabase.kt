package com.dealfinder.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.dealfinder.data.dao.BookingDao
import com.dealfinder.data.dao.CategoryDao
import com.dealfinder.data.dao.FavoriteDao
import com.dealfinder.data.dao.OfferDao
import com.dealfinder.data.dao.ProviderDao
import com.dealfinder.data.dao.SavedOfferDao
import com.dealfinder.data.dao.ServiceDao
import com.dealfinder.data.dao.UserDao
import com.dealfinder.data.dao.VerifiedRatingDao
import com.dealfinder.data.models.Booking
import com.dealfinder.data.models.Category
import com.dealfinder.data.models.DealfinderTypeConverters
import com.dealfinder.data.models.Favorite
import com.dealfinder.data.models.Offer
import com.dealfinder.data.models.Provider
import com.dealfinder.data.models.SavedOffer
import com.dealfinder.data.models.Service
import com.dealfinder.data.models.User
import com.dealfinder.data.models.VerifiedRating

/**
 * Room database for Dealfinder app
 */
@Database(
    entities = [
        User::class,
        Provider::class,
        Service::class,
        VerifiedRating::class,
        Offer::class,
        Booking::class,
        Favorite::class,
        Category::class,
        SavedOffer::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(DealfinderTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun userDao(): UserDao
    abstract fun providerDao(): ProviderDao
    abstract fun serviceDao(): ServiceDao
    abstract fun verifiedRatingDao(): VerifiedRatingDao
    abstract fun offerDao(): OfferDao
    abstract fun bookingDao(): BookingDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun categoryDao(): CategoryDao
    abstract fun savedOfferDao(): SavedOfferDao
    
    companion object {
        const val DATABASE_NAME = "dealfinder_db"
    }
}
